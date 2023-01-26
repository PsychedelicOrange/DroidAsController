package DroidAsControllerServer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.BitSet;
import redlaboratory.jvjoyinterface.VJoy;
import redlaboratory.jvjoyinterface.VjdStat;
import java.util.Arrays;
import java.util.HashMap;
import static DroidAsControllerServer.Constants.*;

import static java.util.Arrays.copyOfRange;
 class Constants
{
    public static final int MAX_DEVICES=14;// vJoy supports 14 virtual devices
}
class Device
{
    public Socket socket; // Device client socket
    public int Did; // device id
    private int Vid; // vJoyid
    public boolean online; // if connection is online
    VJoy vJoy; // vjoy instance

    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    Device(Socket socket,int Did,VJoy vJoy)
    {
        this.socket = socket;
        this.Did = Did;
        Vid = Did+1;
        this.vJoy = vJoy;
        online = true;
        try
        {
            this.dataInputStream = new DataInputStream( new BufferedInputStream( this.socket.getInputStream() ) );
            this.dataOutputStream = new DataOutputStream( new BufferedOutputStream( this.socket.getOutputStream() ) );
        }
        catch ( IOException e )
        {
            System.out.println( "failed to create streams" );
            e.printStackTrace();
        }
        acquireVjoydevice();
        work();
    }

    private void acquireVjoydevice() {
        VjdStat status = vJoy.getVJDStatus(Vid);
        if ((status == VjdStat.VJD_STAT_OWN) ||
                ((status == VjdStat.VJD_STAT_FREE) && (!vJoy.acquireVJD(Vid)))) {
            System.out.println("Failed to acquire vJoy device number" + Vid);
        } else {
            System.out.println("Acquired: vJoy device number "+ Vid);
        }
        int numButtons = vJoy.getVJDButtonNumber(Vid);
        System.out.println( "Number of buttons : "+ numButtons );
    }

    private void work() {
        byte[] bytes = new byte[12];
        while (true) {
            try {
                bytes = dataInputStream.readNBytes(12);
                //System.out.println(bytes);
                vJoy.setAxis(mapAxis(bytes[9]), Vid, VJoy.HID_USAGE_X);
                vJoy.setAxis(mapAxis(bytes[11]), Vid, VJoy.HID_USAGE_Y);

                vJoy.setAxis(mapAxis(bytes[5]), Vid, VJoy.HID_USAGE_RX);
                vJoy.setAxis(mapAxis(bytes[7]), Vid, VJoy.HID_USAGE_RY);

                vJoy.setAxis((Byte.toUnsignedInt(bytes[2])) * 128, Vid, VJoy.HID_USAGE_Z);
                vJoy.setAxis((Byte.toUnsignedInt(bytes[3])) * 128, Vid, VJoy.HID_USAGE_RZ);
                for (int i = 0; i < 8; i++) {
                    vJoy.setBtn(getBit(bytes[0], i), Vid, i + 1);
                }
                for (int i = 8; i < 16; i++) {
                    vJoy.setBtn(getBit(bytes[1], i - 8), Vid, i + 1);
                }

            } catch (Exception e) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                online = false;
                System.out.println( "Disconnected" );
                //relinquish vjoy device
                vJoy.relinquishVJD(Vid);
                break;
            }
        }
    }
    private Boolean getBit(byte mByte, int bit)
    {
        return (mByte & (0b1 << bit)) != 0;
    }
    private long mapAxis(Byte mByte)
    {
        float cons = 16834/128; // = 128 !! 😲
        return (long) ((mByte.longValue()+128)*cons);
    }

}
public class DroidAsController {
    private ServerSocket socket;
    private int idGenerator = 0;
    ArrayList<Device> devices;
    HashMap<InetAddress, Integer> IPtoIDmap;
    VJoy vJoy;
    byte[] bytes = new byte[12];
    JCheckBox[] GUIdevices;
    public static void main(String []args) {
        DroidAsController dac = new DroidAsController();
        dac.IPtoIDmap = new HashMap<InetAddress,Integer>();
        dac.devices = new ArrayList<Device>(MAX_DEVICES);
        HideToSystemTray f =new HideToSystemTray();
        f.setTitle("DroidAsController (Server)");
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    if(dac.socket != null)
                        dac.socket.close();
                    System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        JPanel panel = new JPanel();
        GridLayout glay = new GridLayout(2,7);
        panel.setLayout(glay);

        Thread listening = new Thread() {
            @Override
            public void run() {
                dac.startListening();
            }
        };
        dac.GUIdevices = new JCheckBox[MAX_DEVICES];
        for(int i=0;i<MAX_DEVICES;i++)
        {
            dac.GUIdevices[i] = new JCheckBox((i+1)+ ":");
            dac.GUIdevices[i].setEnabled(false);
            dac.GUIdevices[i].setBackground(Color.red);
            dac.GUIdevices[i].setHorizontalTextPosition(SwingConstants.LEFT);
            panel.add(dac.GUIdevices[i]);
        }
        listening.start();

        f.add(panel);f.pack();
        f.setSize(400,250);
        f.setLayout(null);
        f.setVisible(true);

    }
    boolean startServer()
    {
        try {
            socket = new ServerSocket(12345);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
        System.out.println("Server Started");
        return true;
    }
    private void startListening() {
        Thread checkDisconnect = new Thread()
        {
            @Override
            public void run() {
                while(true)
                {
                    try {
                        for (Device device : devices) {
                            if(device.online)
                                GUIdevices[device.Did].setBackground(Color.green);
                            else
                                GUIdevices[device.Did].setBackground(Color.red);
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }; checkDisconnect.start();
        while(true)
        {
            System.out.println("Listening...");
            try {
                Socket clientSocket = socket.accept(); // listen for connections
                System.out.println("Connection from "+clientSocket.getInetAddress());
                // Assign new IDs for new IPs and consistent IDs for reconnecting IPs
                if(IPtoIDmap.containsKey(clientSocket.getInetAddress()))
                {
                    int previousId = IPtoIDmap.get(clientSocket.getInetAddress());
                    Thread device = new Thread(){
                        public void run()
                        {
                            devices.set(previousId,new Device(clientSocket, previousId,vJoy));
                        }
                    };
                    device.start();
                    GUIdevices[previousId].setBackground(Color.green);
                }
                else
                {
                    IPtoIDmap.put(clientSocket.getInetAddress(),idGenerator);
                    int temp = idGenerator;
                    Thread device = new Thread() {
                        public void run() {
                            devices.add(new Device(clientSocket, temp,vJoy));
                        }
                    };
                    device.start();
                    GUIdevices[idGenerator].setBackground(Color.green);
                    idGenerator +=1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    void initVJoy() {
        vJoy = new VJoy();
        //vJoy.vJoyEnabled());
        if (!vJoy.vJoyEnabled()) {
            System.out.println("vJoy driver not enabled: Failed Getting vJoy attributes.");
            return;
        } else {
            System.out.println("vJoy is enabled.");
            System.out.println("Vendor: " + vJoy.getvJoyManufacturerString());
            System.out.println("Product: " + vJoy.getvJoyProductString());
            System.out.println("Version Number: " + vJoy.getvJoyVersion());
        }
        //vJoy.driverMatch());
        if (vJoy.driverMatch()) {
            System.out.println("Version of Driver Matches DLL Version {0}");
        } else {
            System.out.println("Version of Driver {0} does NOT match DLL Version {1}");
        }
    }

    public DroidAsController()
    {
        initVJoy();
        this.devices = new ArrayList<Device>(MAX_DEVICES);
        this.startServer();
    }
}
