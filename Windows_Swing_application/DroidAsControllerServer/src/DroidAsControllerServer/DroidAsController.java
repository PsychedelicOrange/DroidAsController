package DroidAsControllerServer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.BitSet;
import redlaboratory.jvjoyinterface.VJoy;
import redlaboratory.jvjoyinterface.VjdStat;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;

public class DroidAsController {
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    VJoy vJoy;
    int rID = 1;
    boolean isConn = false;
    String deviceIP = "";
    byte[] bytes = new byte[12];

    public static void main(String []args) {
        DroidAsController dac = new DroidAsController();
        HideToSystemTray f =new HideToSystemTray();
        f.setTitle("DroidAsController (Client)");
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
        HintTextField t = new HintTextField("Enter Device IP:");t.setBounds(130,70,100,20);

        JButton b = new JButton("Connect"); b.setBounds(130,100,100, 20);
        JLabel label = new JLabel("Log");label.setBounds(130,160,150,20);
        JCheckBox c = new JCheckBox("Auto-Connect");c.setBounds(130,130,150,20);
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                    dac.deviceIP = t.getText();
                    Thread network = new Thread() {
                        @Override
                        public void run() {
                            while (!dac.isConn) {
                                dac.isConn = dac.connect();
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    network.start();
                    Thread work = new Thread() {
                        @Override
                        public void run() {
                            try {
                                network.join();
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                            dac.work();
                            if(c.isSelected())
                                b.doClick();
                        }
                    };
                    work.start();
            }
        });
        f.add(t);f.add(b);f.add(c);f.add(label);
        f.setSize(400,250);
        f.setLayout(null);
        f.setVisible(true);

    };
    private Boolean getBit(byte mByte, int bit)
    {
        return (mByte & (0b1 << bit)) != 0;
    }
    private long mapAxis(Byte mByte)
    {
        float cons = 16834/128; // = 128 !! 😲
        return (long) ((mByte.longValue()+128)*cons);
    }

    boolean connect()
    {
        try
        {
            // create new socket and connect to the server
            this.socket  = new Socket();
            socket.connect(new InetSocketAddress(deviceIP, 12345), 10000);
        }
        catch( Exception e )
        {
            System.out.println( "failed to create socket" );
            e.printStackTrace();
            return false;
        }
        System.out.println( "Connected" );
        try
        {
            this.dataInputStream = new DataInputStream( new BufferedInputStream( this.socket.getInputStream() ) );
            this.dataOutputStream = new DataOutputStream( new BufferedOutputStream( this.socket.getOutputStream() ) );
        }
        catch ( IOException e )
        {
            System.out.println( "failed to create streams" );
            e.printStackTrace();
            return false;
        }
        return true;
    }
    void initVJoy()
    {
        vJoy = new VJoy();
        rID = 1;
        //vJoy.vJoyEnabled());
        if (!vJoy.vJoyEnabled()) {
            System.out.println("vJoy driver not enabled: Failed Getting vJoy attributes.");

            return;
        } else {
            System.out.println("Vender: " + vJoy.getvJoyManufacturerString());
            System.out.println("Product: " + vJoy.getvJoyProductString());
            System.out.println("Version Number: " + vJoy.getvJoyVersion());
        }

        //vJoy.driverMatch());
        if (vJoy.driverMatch()) {
            System.out.println("Version of Driver Matches DLL Version {0}");
        } else {
            System.out.println("Version of Driver {0} does NOT match DLL Version {1}");
        }

        VjdStat status = vJoy.getVJDStatus(rID);
        if ((status == VjdStat.VJD_STAT_OWN) ||
                ((status == VjdStat.VJD_STAT_FREE) && (!vJoy.acquireVJD(rID)))) {
            System.out.println("Failed to acquire vJoy device number" + rID);
        } else {
            System.out.println("Acquired: vJoy device number "+ rID);
        }
        int numButtons = vJoy.getVJDButtonNumber(rID);
        System.out.println( "Number of buttons : "+ numButtons );
    }
    void work() {
        while (true) {
            try {
                bytes = dataInputStream.readNBytes(12);
                vJoy.setAxis(mapAxis(bytes[9]), rID, VJoy.HID_USAGE_X);
                vJoy.setAxis(mapAxis(bytes[11]), rID, VJoy.HID_USAGE_Y);

                vJoy.setAxis(mapAxis(bytes[5]), rID, VJoy.HID_USAGE_RX);
                vJoy.setAxis(mapAxis(bytes[7]), rID, VJoy.HID_USAGE_RY);

                vJoy.setAxis((Byte.toUnsignedInt(bytes[2])) * 128, rID, VJoy.HID_USAGE_Z);
                for (int i = 0; i < 8; i++) {
                    vJoy.setBtn(getBit(bytes[0], i), rID, i + 1);
                }
                for (int i = 8; i < 16; i++) {
                    vJoy.setBtn(getBit(bytes[1], i - 8), rID, i + 1);
                }

            } catch (Exception e) {
                isConn = false;
                System.out.println( "Failed to read data" );
                break;
                //e.printStackTrace();
            }
        }
    }
    public DroidAsController()
    {
        initVJoy();
    }
}
