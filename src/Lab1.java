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
            e.printStackTrace();
            System.exit(1);
        }

        // Semaphores for different critical sections
        Semaphore a = new Semaphore(1);
        Semaphore b = new Semaphore(1);
        Semaphore c = new Semaphore(1);
        Semaphore d = new Semaphore(1);
        Semaphore e = new Semaphore(1);
        // For crossing section
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
            public boolean checkSensor(int xPos, int yPos, SensorEvent se, boolean direction) {
                return se.getXpos() == xPos && se.getYpos() == yPos && se.getStatus() == SensorEvent.ACTIVE && direction;
            }

            // Identify the sensor and check the status
            public boolean checkSensor(int xPos, int yPos, SensorEvent se) {
                return se.getXpos() == xPos && se.getYpos() == yPos && se.getStatus() == SensorEvent.ACTIVE;
            }

            // When reaching the sensor, stop first before acquiring the permit. Then set the direction for the switch and resume speed.
            public void getPermission(int id, int speed, int switchPosX, int switchPosY, int switchDirection, Semaphore sem, TSimInterface tsi) {
                try {
                    tsi.setSpeed(id, 0);
                    sem.acquire();
                    tsi.setSwitch(switchPosX, switchPosY, switchDirection);
                    tsi.setSpeed(id, speed);
                } catch (CommandException | InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            // Check if the semaphore is held by a train
            public boolean isSemaphoreHeld(Semaphore semaphore) {
                return semaphore.availablePermits() == 0;
            }

            @Override
            public void run() {
                try {
                    // Initialize: Acquire the semaphore for train 1 and 2
                    a.acquire();
                    e.acquire();

                    // Main part
                    while (true) {
                        SensorEvent se = tsi.getSensor(id);
                        if (se.getStatus() == SensorEvent.INACTIVE) {
                            continue;
                        }

                        // From North to South; Direction: True

                        // From A1 to B
                        if (checkSensor(15, 7, se, direction)) {
                            getPermission(id, speed, 17, 7, TSimInterface.SWITCH_RIGHT, b, tsi);
                        }

                        // From A2 to B
                        if (checkSensor(16, 8, se, direction)) {
                            getPermission(id, speed, 17, 7, TSimInterface.SWITCH_RIGHT, b, tsi);
                        }

                        // Has arrived B, need to release the semaphore a
                        if (checkSensor(19, 7, se, direction)) {
                            // If the train comes from a parallel track and holds a semaphore, it must release the semaphore resource.
                            if (isSemaphoreHeld(a)) {
                                a.release();
                            }
                        }

                        // From B to C
                        if(checkSensor(17,9,se,direction)) {
                            // Stop the train first
                            tsi.setSpeed(id,0);
                            // Make a decision at the switch
                            // If the main road is not occupied, it should be used as the primary route.
                            if(c.tryAcquire()){
                                // On track C1
                                tsi.setSwitch(15,9,TSimInterface.SWITCH_RIGHT);
                            } else {
                                // On track C2
                                tsi.setSwitch(15,9,TSimInterface.SWITCH_LEFT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id,speed);
                        }

                        // Has arrived C1, need to release the semaphore b
                        if (checkSensor(13,9,se,direction)) {
                            b.release();
                        }

                        // Has arrived C1, need to release the semaphore b
                        if (checkSensor(13,0,se,direction)) {
                            b.release();
                        }





                        

















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

