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
            // Set initial speeds for both trains
            tsi.setSpeed(1, speed1);
            tsi.setSpeed(2, speed2);
        } catch (CommandException e) {
            e.printStackTrace();
            System.exit(1);
        }

        // Semaphore initialization based on initial train positions
        // Train 1 starts on A1, Train 2 starts on E1 - these tracks are marked as unavailable (0)
        // to prevent collisions. Available tracks (A2, E2) are set to 1 to guide initial routing.

        // North Station
        Semaphore a1 = new Semaphore(0);  // A1 track initially unavailable to avoid conflicts
        Semaphore a2 = new Semaphore(1);  // A2 track initially available for use

        // South Station
        Semaphore e1 = new Semaphore(0);  // E1 track initially unavailable (switch not pointing here)
        Semaphore e2 = new Semaphore(1);  // E2 track initially available (switch points here)

        Semaphore b = new Semaphore(1);
        Semaphore c1 = new Semaphore(1);
        Semaphore c2 = new Semaphore(1);
        Semaphore d = new Semaphore(1);
        // Crossing control
        Semaphore f = new Semaphore(1);   // Crossing section mutual exclusion


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

            // When reaching the sensor, stop first. Then acquire the permit and release the semaphore. Then set the direction for the switch and resume speed.
            public void acquireAndRelease(int id, int speed, int switchPosX, int switchPosY, int switchDirection, Semaphore semToAcquire, Semaphore semToRelease) {
                try {
                    tsi.setSpeed(id, 0);
                    semToAcquire.acquire();
                    semToRelease.release();
                    tsi.setSwitch(switchPosX, switchPosY, switchDirection);
                    tsi.setSpeed(id, speed);
                } catch (CommandException | InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            @Override
            public void run() {
                try {
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
                        // From A1 to B, and release the semaphore a1
                        if (checkSensor(14, 7, se, direction)) {
                            acquireAndRelease(id, speed, 17, 7, TSimInterface.SWITCH_RIGHT, b, a1);
                        }

                        // From A2 to B, and release the semaphore a2
                        if (checkSensor(15, 8, se, direction)) {
                            acquireAndRelease(id, speed, 17, 7, TSimInterface.SWITCH_LEFT, b, a2);
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
                            if (c1.tryAcquire()) {
                                // On track C1
                                tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
                            } else {
                                // On track C2
                                c2.acquire();
                                tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived C1, need to release the semaphore b
                        if (checkSensor(12, 9, se, direction)) {
                            b.release();
                        }

                        // Has arrived C2, need to release the semaphore b
                        if (checkSensor(13, 10, se, direction)) {
                            b.release();
                        }

                        /**
                         * C --> D
                         */
                        // From C1 to D, and release the semaphore c1
                        if (checkSensor(7, 9, se, direction)) {
                            acquireAndRelease(id, speed, 4, 9, TSimInterface.SWITCH_LEFT, d, c1);
                        }

                        // From C2 to D, and release the semaphore c2
                        if (checkSensor(6, 10, se, direction)) {
                            acquireAndRelease(id, speed, 4, 9, TSimInterface.SWITCH_RIGHT, d, c2);
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
                            if (e1.tryAcquire()) {
                                // On track E1
                                tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
                            } else {
                                // On track E2
                                e2.acquire();
                                tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived E1, need to release the semaphore d
                        if (checkSensor(6, 11, se, direction)) {
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
                        // From E1 to D, and release the semaphore e1
                        if (checkSensor(6, 11, se, !direction)) {
                            acquireAndRelease(id, speed, 3, 11, TSimInterface.SWITCH_LEFT, d, e1);
                        }

                        // From E2 to D, and release the semaphore e2
                        if (checkSensor(4, 13, se, !direction)) {
                            acquireAndRelease(id, speed, 3, 11, TSimInterface.SWITCH_RIGHT, d, e2);
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
                            if (c1.tryAcquire()) {
                                // On track C1
                                tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
                            } else {
                                // On track C2
                                c2.acquire();
                                tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived C1, need to release the semaphore d
                        if (checkSensor(7, 9, se, !direction)) {
                            d.release();
                        }

                        // Has arrived C1, need to release the semaphore d
                        if (checkSensor(6, 10, se, !direction)) {
                            d.release();
                        }

                        /**
                         * C --> B
                         */
                        // From C1 to B, and release the semaphore c1
                        if (checkSensor(12, 9, se, !direction)) {
                            acquireAndRelease(id, speed, 15, 9, TSimInterface.SWITCH_RIGHT, b, c1);
                        }

                        // From C2 to B, and release the semaphore c2
                        if (checkSensor(13, 10, se, !direction)) {
                            acquireAndRelease(id, speed, 15, 9, TSimInterface.SWITCH_LEFT, b, c2);
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
                            if (a1.tryAcquire()) {
                                // On track A1
                                tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
                            } else if (a2.tryAcquire()) {
                                // On track A2
                                tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
                            }
                            // Resume the speed
                            tsi.setSpeed(id, speed);
                        }

                        // Has arrived A1, need to release the semaphore b
                        if (checkSensor(14, 7, se, !direction)) {
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
                        if (checkSensor(6, 6, se)) {
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
                        if (checkSensor(11, 7, se)) {
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
                        if (checkSensor(9, 5, se)) {
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
                        if (checkSensor(10, 8, se)) {
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

