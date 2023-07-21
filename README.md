# OS-Students-TA-Problem
Simulates the popular Operating Systems "Sleeping TA Problem" or "Sleeping Barber Problem" with thread synchronization.

## Explanation

This problem simulates the Sleeping Teaching Assistant Problem as follows.

1. The problem consists of one TA, one chair in the TA's office for a
student, and three chairs outside of the TA's office for students to wait
until the TA is ready to serve the next student.

2. If a student arrives, verifies that the waiting area is empty, checks the
TA's office, and sees that the TA is sleeping, the student will wake the TA
up and sit on the chair in the office.

3. If a student arrives, verifies that the waiting area is empty, checks the
TA's office, and sees that the TA is currently occupied with another student,
the student will wait by sitting in one of the three available chairs in the
waiting area.

4. If a student arrives, sees that there are students in the waiting area,
the student will have a seat and wait.

5. if a student arrives, sees that there the waiting area is full, the
student will leave.

6. If the TA finishes answering a student's questions, the TA will check the
waiting area for any students. If there is a student, the TA will call a
student into the office. if there are no students, the TA will go back to
sleep.

---

PROBLEM 1: Deadlock
- There may be a scenario where a student ends up waiting for the TA, and
the TA is waiting for a student. That is, the TA is sleeping while a student
in the waiting area is waiting to be called in by the TA.

PROBLEM 2: Starvation
- May happen when a student is in the waiting area and never gets called,
if the TA calls in a student at random.

Solution: Semaphores, mutex (binary semaphore), integer counters
- Seat counter
    -  Counts the number of seats available in the waiting area
- Seat mutex:
    - A seat counter can only be modified by the student who holds the mutex
- TA status semaphore:
    - Tells the status of the TA (available or busy)
- TA attention semaphore:
    - Represents which student gets to have the TA's attention
    - Students can join the queue by holding the current semaphore's value
