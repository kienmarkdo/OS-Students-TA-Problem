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

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdbool.h>
#include <pthread.h>
#include <semaphore.h>

#define NUM_SEATS 3

/* Global Variables */
int NUM_STUDENTS; // total # of students requiring the TA's help
int availableSeats = NUM_SEATS + 1; // # of seats currently available (waiting area + TA's office)
sem_t canModifySeats; // mutex to allow modification of seatsAvailable
sem_t taStatus; // semaphore to check the status of the TA (available/busy)
sem_t taAttention; // semaphore queue to know who has the TA's attention
int numOfStudentsHelped = 0; // if this reaches NUM_STUDENTS, that means that all students have been helped

/* Function Prototypes */
void* taActivities(void* arg);
void* studentActivities(void* arg);

int main(int argc, char** argv) {
    if (argc != 2 || atoi(argv[1]) < 0) {
        fprintf(stderr, "ERROR: Command-line argument requires one positive integer to represent the number of total students.\n"
                        "Example: gcc -o students_and_ta students_and_ta.c ; ./students_and_ta 10\n");
        return 1;
    }

    NUM_STUDENTS = atoi(argv[1]);

    printf("================== Sleeping TA Simulation ==================\n");
    printf("Total number of students who require help: %d\n", NUM_STUDENTS);
    printf("The TA must help all %d students.\n", NUM_STUDENTS);
    printf("Each student will attempt to get the TA's help or sit in the waiting area until it is their turn to be helped by the TA.\n");
    printf("Once a student receives help from the TA, the student will leave and never come back.\n");
    printf("If a student arrives and sees that there are no seats left in the waiting area, the student will leave and come back later.\n");
    printf("Each time the student comes and sees that the waiting area is full, the student will leave for a longer period of time.\n");
    printf("==================    Simulation START    ==================\n");

    pthread_t taThread;
    pthread_t studentThreads[NUM_STUDENTS];

    sem_init(&canModifySeats, 0, 1);
    sem_init(&taStatus, 0, 0);
    sem_init(&taAttention, 0, 0);

    for (int i = 0; i < NUM_STUDENTS; i++) {
        int* studentNumber = (int*)malloc(sizeof(int));
        *studentNumber = i + 1;
        pthread_create(&studentThreads[i], NULL, studentActivities, (void*)studentNumber);
    }
    pthread_create(&taThread, NULL, taActivities, NULL);

    pthread_join(taThread, NULL);

    printf("==================     Simulation END     ==================\n");
    return 0;
}

void* taActivities(void* arg) {
    printf("The TA is in his/her office\n");

    while (numOfStudentsHelped != NUM_STUDENTS) {
        printf("TA is sleeping...\n");
        sem_wait(&taStatus); // TA is waiting to be woken up
        printf("TA is woken up by student ");
        sem_post(&taAttention); // TA is now woken up - TA gives attention to the student in need of help
        sem_wait(&taStatus); // TA goes back to sleep and waits to be open up
        sem_wait(&taAttention); // TA keeps attention to self until a student arrives
    }

    printf("The TA has helped all of the students. The TA will now leave the office and go home.\n");
    return NULL;
}

void* studentActivities(void* arg) {
    int studentNumber = *((int*)arg);
    int waitingTimeMilliseconds = 1000; // time for which student will leave if help is not received
    int attemptsTaken = 0; // # of attempts taken to get the TA's help

    while (attemptsTaken++ != -1) {
        sem_wait(&canModifySeats);
        if (availableSeats > 0) {
            if (availableSeats-- == NUM_SEATS + 1) { // TA is sleeping
                sem_post(&taStatus); // wake the TA up
                sem_wait(&taAttention);
                printf("%d\n", studentNumber); // finishing the printf from TA routine
                sem_post(&canModifySeats); // gets off the chair, goes into office, give seat mutex to other students
            } else {
                printf("Student %d sits in waiting area\n", studentNumber);
                sem_post(&canModifySeats); // sat down. Give seat mutex to another student
                sem_wait(&taAttention); // student patiently waits for the TA's attention
            }

            printf("Student %d now receiving help from TA\n", studentNumber);

            usleep((rand() % 1000) * 1000); // student receives help for a random time

            sem_wait(&canModifySeats); // take back seat mutex to increment available seats
            availableSeats++; // seat in TA's office is now available
            printf("Student %d DONE receiving help from TA\n", studentNumber);

            if (availableSeats == NUM_SEATS + 1) { // no students waiting in the area nor in TA office
                sem_post(&taStatus); // allow the TA goes back to sleep
            }
            sem_post(&taAttention); // give the TA his/her attention back
            sem_post(&canModifySeats); // make seat mutex available for incoming students to use

            attemptsTaken = -1; // signal that the student has successfully received help from the TA
            numOfStudentsHelped++; // increment the total number of students the TA has helped
        } else { // no seats left in the waiting area; leave without help from TA
            printf("Student %d leaves as there are no seats left - Student will return later "
                    "(attempt %d)\n", studentNumber, attemptsTaken);
            sem_post(&canModifySeats); // release the lock on the seats

            usleep(waitingTimeMilliseconds * 1000); // student leaves for a period of time
            waitingTimeMilliseconds = waitingTimeMilliseconds * 2; // student's leave time is longer
        }
    }

    free(arg);
    return NULL;
}
