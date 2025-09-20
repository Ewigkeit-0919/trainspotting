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
            public void getPermission(int id, int speed, int switchPosX, int switchPosY, int switchDirection, Semaphore sem) {
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
                    if (id == 1)
                        a.acquire();
                    else
                        e.acquire();

                    // Main part
                    while (true) {
                        SensorEvent se = tsi.getSensor(id);
                        if (se.getStatus() == SensorEvent.INACTIVE) {
                            continue;
                        }

                        /**
                         * From North to South
                         * Direction: True
                         */

                        /**
                         * A --> B
                         */
                        // From A1 to B
                        if (checkSensor(15, 7, se, direction)) {
                            getPermission(id, speed, 17, 7, TSimInterface.SWITCH_RIGHT, b);
                        }

                        // From A2 to B
                        if (checkSensor(15, 8, se, direction)) {
                            getPermission(id, speed, 17, 7, TSimInterface.SWITCH_LEFT, b);
                        }

                        // Has arrived B, need to release the semaphore a
                        if (checkSensor(19, 7, se, direction)) {
                            // If the train comes from a parallel track and holds a semaphore, it must release the semaphore resource.
                            if (isSemaphoreHeld(a)) {
                                a.release();
                            }
                        }

                        /**
                         * B --> C
                         */
                        // From B to C
                        if (checkSensor(17, 9, se, direction)) {
                            // Stop the train first
                            tsi.setSpeed(id, 0);
                            // Make a decision at the switch
                            // If the main road is not occupied, it should be used as the primary route.
                            if (c.tryAcquire()) {
                                // On track C1
                                tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
                            } else {
                                // On track C2
                                tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived C1, need to release the semaphore b
                        if (checkSensor(13, 9, se, direction)) {
                            b.release();
                        }

                        // Has arrived C2, need to release the semaphore b
                        if (checkSensor(13, 10, se, direction)) {
                            b.release();
                        }

                        /**
                         * C --> D
                         */
                        // From C1 to D
                        if (checkSensor(6, 9, se, direction)) {
                            getPermission(id, speed, 4, 9, TSimInterface.SWITCH_LEFT, d);
                        }

                        // From C2 to D
                        if (checkSensor(6, 10, se, direction)) {
                            getPermission(id, speed, 4, 9, TSimInterface.SWITCH_RIGHT, d);
                        }

                        // Has arrived D, need to release the semaphore c
                        if (checkSensor(2, 9, se, direction)) {
                            // If the train comes from a parallel track and holds a semaphore, it must release the semaphore resource.
                            if (isSemaphoreHeld(c)) {
                                c.release();
                            }
                        }

                        /**
                         * D --> E
                         */
                        // From D to E (south station)
                        if (checkSensor(1, 11, se, direction)) {
                            // Stop the train first
                            tsi.setSpeed(id, 0);
                            // Make a decision at the switch
                            // If the main road is not occupied, it should be used as the primary route.
                            if (e.tryAcquire()) {
                                // On track E1
                                tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
                            } else {
                                // On track E2
                                tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived E1, need to release the semaphore d
                        if (checkSensor(5, 11, se, direction)) {
                            d.release();
                        }

                        // Has arrived E2, need to release the semaphore d
                        if (checkSensor(4, 13, se, direction)) {
                            d.release();
                        }

                        /**
                         * From South to North
                         * Direction: False
                         */

                        /**
                         * E --> D
                         */
                        // From E1 to D
                        if (checkSensor(5, 11, se, !direction)) {
                            getPermission(id, speed, 3, 11, TSimInterface.SWITCH_LEFT, d);
                        }

                        // From E2 to D
                        if (checkSensor(4, 13, se, !direction)) {
                            getPermission(id, speed, 3, 11, TSimInterface.SWITCH_RIGHT, d);
                        }

                        // Has arrived D, need to release the semaphore e
                        if (checkSensor(1, 11, se, !direction)) {
                            // If the train comes from a parallel track and holds a semaphore, it must release the semaphore resource.
                            if (isSemaphoreHeld(e)) {
                                e.release();
                            }
                        }

                        /**
                         * D --> C
                         */
                        // From D to C
                        if (checkSensor(2, 9, se, !direction)) {
                            // Stop the train first
                            tsi.setSpeed(id, 0);
                            // Make a decision at the switch
                            // If the main road is not occupied, it should be used as the primary route.
                            if (c.tryAcquire()) {
                                // On track C1
                                tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
                            } else {
                                // On track C2
                                tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived C1, need to release the semaphore d
                        if (checkSensor(6, 9, se, !direction)) {
                            d.release();
                        }

                        // Has arrived C1, need to release the semaphore d
                        if (checkSensor(6, 10, se, !direction)) {
                            d.release();
                        }

                        /**
                         * C --> B
                         */
                        // From C1 to B
                        if (checkSensor(13, 9, se, !direction)) {
                            getPermission(id, speed, 15, 9, TSimInterface.SWITCH_RIGHT, b);
                        }

                        // From C2 to B
                        if (checkSensor(13, 10, se, !direction)) {
                            getPermission(id, speed, 15, 9, TSimInterface.SWITCH_LEFT, b);
                        }

                        // Has arrived B, need to release the semaphore c
                        if (checkSensor(17, 9, se, !direction)) {
                            // If the train comes from a parallel track and holds a semaphore, it must release the semaphore resource.
                            if (isSemaphoreHeld(c)) {
                                c.release();
                            }
                        }

                        /**
                         * B --> A
                         */
                        // From B to A
                        if (checkSensor(19, 7, se, !direction)) {
                            // Stop the train first
                            tsi.setSpeed(id, 0);
                            // Make a decision at the switch
                            // If the main road is not occupied, it should be used as the primary route.
                            if (a.tryAcquire()) {
                                // On track A1
                                tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
                            } else {
                                // On track A2
                                tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived A1, need to release the semaphore b
                        if (checkSensor(15, 7, se, !direction)) {
                            b.release();
                        }

                        // Has arrived A1, need to release the semaphore b
                        if (checkSensor(15, 8, se, !direction)) {
                            b.release();
                        }

                        /**
                         * Crossing
                         */

                        // Between A1 and F, from north to south
                        if (checkSensor(6, 7, se)) {
                            if (direction) {
                                // If the direction is true, meaning from A1 to F
                                // Stop the train first
                                tsi.setSpeed(id, 0);
                                // Get the semaphore
                                f.acquire();
                                // Resume the speed
                                tsi.setSpeed(id, speed);
                            } else {
                                // If the direction is false, meaning from F to A1
                                // Release the semaphore
                                f.release();
                            }
                        }

                        // Between A1 and F, from south to north
                        if (checkSensor(10, 7, se)) {
                            if (!direction) {
                                // If the direction is false, meaning from A1 to F
                                // Stop the train first
                                tsi.setSpeed(id, 0);
                                // Get the semaphore
                                f.acquire();
                                // Resume the speed
                                tsi.setSpeed(id, speed);
                            } else {
                                // If the direction is false, meaning from F to A1
                                // Release the semaphore
                                f.release();
                            }
                        }

                        // Between A2 and F, from north to south
                        if (checkSensor(8, 5, se)) {
                            if (direction) {
                                // If the direction is true, meaning from A2 to F
                                // Stop the train first
                                tsi.setSpeed(id, 0);
                                // Get the semaphore
                                f.acquire();
                                // Resume the speed
                                tsi.setSpeed(id, speed);
                            } else {
                                // If the direction is false, meaning from F to A2
                                // Release the semaphore
                                f.release();
                            }
                        }

                        // Between A2 and F, from south to north
                        if (checkSensor(9, 8, se)) {
                            if (!direction) {
                                // If the direction is false, meaning from A2 to F
                                // Stop the train first
                                tsi.setSpeed(id, 0);
                                // Get the semaphore
                                f.acquire();
                                // Resume the speed
                                tsi.setSpeed(id, speed);
                            } else {
                                // If the direction is false, meaning from F to A2
                                // Release the semaphore
                                f.release();
                            }
                        }

                        /**
                         * Stop at stations and turn back
                         */
                        // Stop and turn back at stations
                        // If the train enters the station
                        if (checkSensor(13, 3, se, !direction) ||
                                checkSensor(13, 5, se, !direction) ||
                                checkSensor(13, 11, se, direction) ||
                                checkSensor(13, 13, se, direction)) {
                            tsi.setSpeed(id, 0);
                            sleep(1000 + (20 * Math.abs(speed)));
                            //turn back
                            speed *= -1;
                            tsi.setSpeed(id, speed);
                            direction = !direction;
                        }

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

