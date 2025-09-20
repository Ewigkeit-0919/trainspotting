import TSim.CommandException;
import TSim.SensorEvent;
import TSim.TSimInterface;
import java.util.concurrent.Semaphore;
import static java.lang.Thread.sleep;

public class Lab1 {

    public Lab1(int speed1, int speed2) {
        TSimInterface tsi = TSimInterface.getInstance();
        tsi.setDebug(true);

        try {
            tsi.setSpeed(1, speed1);
            tsi.setSpeed(2, speed2);
        } catch (CommandException e) {
            e.printStackTrace();    // or only e.getMessage() for the error
            System.exit(1);
        }

        // semaphores for different critical sections
        Semaphore a = new Semaphore(1);
        Semaphore b = new Semaphore(1);
        Semaphore c = new Semaphore(1);
        Semaphore d = new Semaphore(1);
        Semaphore e = new Semaphore(1);
        // for crossing section
        Semaphore f = new Semaphore(1);


        class Train implements Runnable {

            int id;
            int speed;
            // True = from north to south
            boolean direction;

            public Train(int id, int speed, boolean direction) {
                this.id = id;
                this.speed = speed;
                this.direction = direction;
            }

            // Identify the sensor and check the status (train direction required)
            public boolean checkSensor (int xPos, int yPos, SensorEvent se, boolean direction) {
                return se.getXpos() == xPos && se.getYpos() == yPos && se.getStatus() == SensorEvent.ACTIVE && direction;
            }

            // Identify the sensor and check the status
            public boolean checkSensor(int xPos, int yPos, SensorEvent se) {
                return se.getXpos() == xPos && se.getYpos() == yPos && se.getStatus() == SensorEvent.ACTIVE;
            }

            @Override
            public void run() {
                try {
                    // initialize: acquire the semaphore for train 1 and 2
                    a.acquire();
                    e.acquire();

                    // main part
                    while (true){
                        SensorEvent se = tsi.getSensor(id);
                        if (se.getStatus() == SensorEvent.INACTIVE) {
                            continue;
                        }

                        // From North to South; Direction: True

                        // From South to North; Direction: False

                        // Stop at stations and Turn back
                    }

                } catch (CommandException | InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

        }

        // from north to south
        Thread train1 = new Thread(new Train(1, speed1, true));
        train1.start();
        // from south to north
        Thread train2 = new Thread(new Train(2, speed2, false));
        train2.start();


    }

}

