import java.util.concurrent.Semaphore;

/**
 * Last name: Do
 * Student number: 300163370
 * 
 * CSI 3131 Operating Systems
 * Assignment 3 - Thread Synchronization
 * 
 * ============================================================================
 * 
 * This problem simulates the Sleeping Teaching Assistant Problem as follows.
 * 
 * 1. The problem consists of one TA, one chair in the TA's office for a
 * student, and three chairs outside of the TA's office for students to wait
 * until the TA is ready to serve the next student.
 * 
 * 2. If a student arrives, verifies that the waiting area is empty, checks the
 * TA's office, and sees that the TA is sleeping, the student will wake the TA
 * up and sit on the chair in the office.
 * 
 * 3. If a student arrives, verifies that the waiting area is empty, checks the
 * TA's office, and sees that the TA is currently occupied with another student,
 * the student will wait by sitting in one of the three available chairs in the
 * waiting area.
 * 
 * 4. If a student arrives, sees that there are students in the waiting area,
 * the student will have a seat and wait.
 * 
 * 5. if a student arrives, sees that there the waiting area is full, the
 * student will leave.
 * 
 * 6. If the TA finishes answering a student's questions, the TA will check the
 * waiting area for any students. If there is a student, the TA will call a
 * student into the office. if there are no students, the TA will go back to
 * sleep.
 * 
 * ============================================================================
 * 
 * PROBLEM 1: Deadlock
 * --- There may be a scenario where a student ends up waiting for the TA, and
 * the TA is waiting for a student. That is, the TA is sleeping while a student
 * in the waiting area is waiting to be called in by the TA.
 * 
 * Problem 2: Starvation
 * --- May happen when a student is in the waiting area and never gets called,
 * if the TA calls in a student at random.
 * 
 * Solution: Semaphores, mutex (binary semaphore), integer counters
 * --- Seat counter
 * ------ Counts the number of seats available in the waiting area
 * --- Seat mutex:
 * ------ A seat counter can only be modified by the student who holds the mutex
 * --- TA status semaphore:
 * ------ Tells the status of the TA (available or busy)
 * --- TA attention semaphore:
 * ------ Represents which student gets to have the TA's attention
 * ------ Students can join the queue by holding the current semaphore's value
 */

public class A3 {
    private static int NUM_STUDENTS; // total # of students requiring the TA's help
    private static final int NUM_SEATS = 3; // total # of seats in the waiting area

    /*
     * NUM_SEATS + 1 means that the TA is sleeping.
     * 
     * Explanation:
     * There are NUM_SEATS available in the waiting room + 1 seat in the TA's
     * office.
     */
    private static int availableSeats = NUM_SEATS + 1; // # of seats currently available (waiting area + TA's office)
    private static Semaphore canModifySeats = new Semaphore(1); // mutex to allow modification of seatsAvailable
    private static Semaphore taStatus = new Semaphore(0); // semaphore to check the status of the TA (available/busy)
    private static Semaphore taAttention = new Semaphore(0); // semaphore queue to know who has the TA's attention
    private static int numOfStudentsHelped = 0; // if this reaches NUM_STUDENTS, that means that all students have been
                                                // helped and the TA can go home

    public static void main(String[] args) {

        // throw an error if user did not input exactly 1 command-line argument that is
        // a positive integer
        if (args.length != 1 || Integer.parseInt(args[0]) < 0) {
            throw new IllegalArgumentException(
                    "ERROR: Command-line argument requires one positive integer to represent the number of total students.\n"
                            + "Example: javac A3.java && java A3 10");
        }

        // set total # of students
        NUM_STUDENTS = Integer.parseInt(args[0]);

        // explain what this program is doing
        System.out
                .format("""
                        ================== Sleeping TA Simulation ==================

                        Total number of students who require help: %d

                        The TA must help all %d students.

                        Each student will attempt to get the TA's help or sit in the waiting area until it is their turn to be helped by the TA.
                        Once a student receives help from the TA, the student will leave and never come back.

                        If a student arrives and sees that there are no seats left in the waiting area, the student will leave and come back later.
                        Each time the student comes and sees that the waiting area is full, the student will leave for a longer period of time.

                        ==================    Simulation START    ==================

                        """
                        .formatted(NUM_STUDENTS, NUM_STUDENTS));

        // TA is represented with 1 thread, students are represented with n threads
        Thread taThread = new Thread(new TeachingAssistant());
        Thread[] studentThreads = new Thread[NUM_STUDENTS];

        for (int i = 0; i < NUM_STUDENTS; i++) {
            studentThreads[i] = new Thread(new Student(i + 1));
            studentThreads[i].start();
        }
        taThread.start();

        try {
            taThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static class TeachingAssistant implements Runnable {
        public void run() {
            System.out.println("The TA is in his/her office");

            while (numOfStudentsHelped != NUM_STUDENTS) { // stop when TA has helped every student
                System.out.println("TA is sleeping...");
                taStatus.acquireUninterruptibly(); // TA is waiting to be woken up
                System.out.print("TA is woken up by student ");
                taAttention.release(); // TA is now woken up - TA gives attention to the student in need of help
                taStatus.acquireUninterruptibly(); // TA goes back to sleep and waits to be open up
                taAttention.acquireUninterruptibly(); // TA keeps attention to self until a student arrives
            }

            System.out.println("The TA has helped all of the students. The TA will now leave the office and go home.");
            System.out.println("==================     Simulation END     ==================");
            System.exit(0); // exit program
        }
    }

    private static class Student implements Runnable {
        private int studentNumber;
        private int waitingTimeMilliseconds = 1000; // time for which student will leave if help is not received
        private int attemptsTaken = 0; // # of attempts taken to get the TA's help

        public Student(int studentNumber) {
            this.studentNumber = studentNumber;
        }

        public void run() {
            while (attemptsTaken++ != -1) { // stop when student has received help from TA
                try {
                    canModifySeats.acquire();
                    if (availableSeats > 0) {
                        if (availableSeats-- == NUM_SEATS + 1) { // TA is sleeping
                            taStatus.release(); // wake the TA up
                            taAttention.acquire();
                            System.out.println(studentNumber); // finishing the println from TA routine
                            canModifySeats.release(); // gets off the chair, goes into office, give seat mutex to other
                                                      // students
                        } else {
                            System.out.println("Student " + studentNumber + " sits in waiting area");
                            canModifySeats.release(); // sat down. Give seat mutex to another student
                            taAttention.acquire(); // student patiently waits for the TA's attention
                        }

                        System.out.println("Student " + studentNumber + " now receiving help from TA");

                        Thread.sleep((long) (Math.random() * 1000)); // student receives help for a random time

                        canModifySeats.acquire(); // take back seat mutex to increment available seats
                        availableSeats++; // seat in TA's office is now available
                        System.out.println("Student " + studentNumber + " DONE receiving help from TA");

                        if (availableSeats == NUM_SEATS + 1) { // no students waiting in the area nor in TA office
                            taStatus.release(); // allow the TA goes back to sleep
                        }
                        taAttention.release(); // give the TA his/her attention back
                        canModifySeats.release(); // make seat mutex available for incoming students to use

                        attemptsTaken = -1; // signal that the student has successfully received help from the TA
                        numOfStudentsHelped++; // increment the total number of students the TA has helped
                    } else { // no seats left in the waiting area; leave without help from TA
                        System.out.println("Student " + studentNumber + " leaves as there are no seats left - "
                                + "Student will return later "
                                + "(attempt " + attemptsTaken + ")");
                        canModifySeats.release(); // release the lock on the seats

                        Thread.sleep(waitingTimeMilliseconds); // student leaves for a period of time
                        waitingTimeMilliseconds = waitingTimeMilliseconds * 2; // student's leave time is longer
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
