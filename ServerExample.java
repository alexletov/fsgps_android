package ru.alexletov;

// Use official FSUIPC SDK
import com.flightsim.fsuipc.FSAircraft;
import com.flightsim.fsuipc.fsuipc_wrapper;
import ru.alexletov.fsgps.helpers.PositionInfoSerializable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Running tests");
        int ret = fsuipc_wrapper.Open(fsuipc_wrapper.SIM_ANY);
        System.out.println("ret = " + ret);

        ServerSocket socket = null;
        try {
            socket = new ServerSocket(10512);
            Socket cli;
            while (true) {
                cli = socket.accept();
                final Socket s = cli;
                new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
                            while (true) {
                                FSAircraft acft = new FSAircraft();
                                PositionInfoSerializable pos = new PositionInfoSerializable(acft.Latitude(),
                                        acft.Longitude(), acft.Altitude(), acft.IAS() / 128,
                                        (float) ((double) acft.Heading() / 65535 / 65535 * 360
                                                - (double) acft.Magnetic() / 65536 * 360));
                                os.writeObject(pos);
                                System.out.println(acft.Altitude() + " " + acft.Latitude() + " " + acft.Longitude()
                                        + " " + acft.IAS() / 128 + " " + ((double) acft.Heading() / 65535 / 65535 * 360
                                        - (double) acft.Magnetic() / 65536 * 360));
                                Thread.sleep(1000);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
