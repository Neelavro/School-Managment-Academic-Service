# School Management System — User Manual

---

## Table of Contents

1. [Getting Started — Login](#1-getting-started--login)
2. [Navigation Overview](#2-navigation-overview)
3. [Setup](#3-setup)
   - 3.1 [System Settings](#31-system-settings)
   - 3.2 [Academic Years](#32-academic-years)
   - 3.3 [Shifts](#33-shifts)
   - 3.4 [Classes](#34-classes)
   - 3.5 [Groups](#35-groups)
   - 3.6 [Sections](#36-sections)
   - 3.7 [Grading Policies](#37-grading-policies)
4. [Students](#4-students)
   - 4.1 [All Students (Student List)](#41-all-students-student-list)
   - 4.2 [Add a New Student](#42-add-a-new-student)
   - 4.3 [Edit a Student](#43-edit-a-student)
   - 4.4 [View Student Details](#44-view-student-details)
   - 4.5 [Archived Students](#45-archived-students)
   - 4.6 [Student Transfer](#46-student-transfer)
   - 4.7 [Student Migration](#47-student-migration)
5. [Exam](#5-exam)
   - 5.1 [Rooms](#51-rooms)
   - 5.2 [Subjects](#52-subjects)
   - 5.3 [Exam Types](#53-exam-types)
   - 5.4 [Exam Routines & Sessions](#54-exam-routines--sessions)
6. [Marking](#6-marking)
   - 6.1 [Exam Components](#61-exam-components)
   - 6.2 [Marking Structures](#62-marking-structures)
   - 6.3 [Mark Entry](#63-mark-entry)
7. [Results](#7-results)
   - 7.1 [Session Results](#71-session-results)
   - 7.2 [Routine Results](#72-routine-results)
   - 7.3 [Annual Results](#73-annual-results)
   - 7.4 [Merit List](#74-merit-list)
   - 7.5 [Statistics](#75-statistics)
8. [Downloads](#8-downloads)
   - 8.1 [Student ID Card](#81-student-id-card)
   - 8.2 [Admit Card](#82-admit-card)
   - 8.3 [Progress Report](#83-progress-report)
   - 8.4 [Seat Plan](#84-seat-plan)
9. [Administration](#9-administration)
10. [Common Rules & Warnings](#10-common-rules--warnings)

---

## 1. Getting Started — Login

### How to log in

1. Open the application in your browser.
2. You will land on the **Login** screen — a white card on a blue gradient background.
3. Enter your **Phone Number** in the first field.
4. Enter your **Password** in the second field.
   - Click the eye icon on the right side of the password field to show or hide the password.
5. Click **Sign In**.

If login is successful you are taken directly to the **Dashboard**.

### Saved Accounts (quick login)

- After a successful login, your credentials are saved locally in your browser.
- Next time you open the login screen, click the **down-arrow icon** on the right side of the phone number field to see your saved accounts.
- Click any saved account to auto-fill both fields, then click **Sign In**.
- To remove a saved account from the list, click the **× (close)** icon next to that account in the dropdown.

### DO NOT

- Do not share your login credentials with anyone.
- Do not use the browser's back button to navigate after logging out; always use the sidebar links.
- Do not close the browser tab mid-operation (while saving a student, uploading a file, etc.) as it can leave data in an incomplete state.

### How to log out

Click the red **Logout** button at the very bottom of the left sidebar.

---

## 2. Navigation Overview

The application has a **fixed sidebar** on the left side with the following sections:

| Section | Items |
|---|---|
| (Top) | Dashboard |
| Setup | System Settings, Academic Years, Shifts, Classes, Groups, Sections, Grading Policies |
| Students | All Students, Archived Students, Student Transfer |
| Exam | Rooms, Subjects, Exam Types, Exam Routines |
| Marking | Exam Components, Marking Structures, Mark Entry |
| Results | Session Results, Routine Results, Annual Results, Merit List, Statistics |
| Downloads | Student ID Card, Admit Card, Progress Report, Seat Plan |
| Administration | Admins |

Each section heading is **expandable**. Click the section title to expand or collapse its menu items.

The currently active page is highlighted in **blue** in the sidebar.

---

## 3. Setup

The Setup section must be configured **before** anything else — students, exams, and results all depend on this data.

> **Recommended order:** System Settings → Academic Years → Shifts → Classes → Groups → Sections → Grading Policies

---

### 3.1 System Settings

**Path:** Setup → System Settings

This page stores your institution's identity information that appears on all generated documents (ID cards, admit cards, progress reports).

#### What you can do

| Field | Description |
|---|---|
| Institution Name | Full name of the school/institution |
| Address | Physical address of the institution |
| Heading | A secondary heading line (e.g., "Established 1980") |
| Logo | Upload the institution's logo (image file) |
| Signature | Upload an authorized signature image |

#### How to update institution details

1. Fill in the **Institution Name**, **Address**, and **Heading** fields.
2. Click **Save Details**.

#### How to update the logo

1. Click **Upload Logo** (or click on the existing logo preview area).
2. Select an image file from your computer (JPG, PNG recommended).
3. Click **Save Logo**.

#### How to update the signature

1. Click **Upload Signature**.
2. Select an image file.
3. Click **Save Signature**.

#### DO NOT

- Do not upload very large image files (keep logo and signature under 2 MB).
- Do not leave the Institution Name blank — it appears on every printed document.
- Do not click Save multiple times in quick succession; wait for the success toast before clicking again.

---

### 3.2 Academic Years

**Path:** Setup → Academic Years

Academic years are the top-level time containers for all student enrollments and exam results.

#### How to add an academic year

1. Click **Add Academic Year** (or the **+** button).
2. Enter the year label (e.g., `2024`, `2023-2024`).
3. Click **Save**.

#### How to edit/delete

- Click the **Edit** icon next to any year to rename it.
- Click the **Delete** icon to remove it.

#### DO NOT

- Do not delete an academic year that already has students enrolled in it — this will break all associated records.
- Do not create duplicate years with the same label.

---

### 3.3 Shifts

**Path:** Setup → Shifts

Shifts represent the time slots in which classes run (e.g., Morning, Day, Evening).

#### How to add a shift

1. Click **Add Shift**.
2. Enter the shift name.
3. Click **Save**.

#### How to toggle active/inactive

- Use the toggle switch next to each shift to activate or deactivate it.
- Inactive shifts will not appear in student enrollment dropdowns.

#### DO NOT

- Do not delete a shift that has classes assigned to it.
- Do not rename a shift to something misleading — the name appears in student lists and on documents.

---

### 3.4 Classes

**Path:** Setup → Classes

Classes are organized under Shifts. When you open this page you see all shifts; click into a shift to manage its classes.

#### How to add a class

1. Click on the desired **Shift** row to drill in.
2. Click **Add Class**.
3. Enter the class name (e.g., `Class 1`, `Grade 6`).
4. Click **Save**.

#### Class ordering

- Classes can be **reordered** by dragging or using the order controls. The display order affects how they appear in dropdown lists throughout the system.

#### DO NOT

- Do not add classes without first selecting the correct shift.
- Do not delete a class that has students enrolled in it.

---

### 3.5 Groups

**Path:** Setup → Groups

Groups (Student Groups) represent subject-group divisions within a class, such as Science, Arts, Commerce.

#### How to add a group

1. Click **Add Group**.
2. Enter the group name.
3. Click **Save**.

#### DO NOT

- Do not create overlapping group names within the same class.
- Do not delete a group that has active student enrollments assigned to it.

---

### 3.6 Sections

**Path:** Setup → Sections

Sections are organized as: **Class → Gender Section → Section**.

- **Gender Section** = a grouping by gender (e.g., Boys, Girls, Combined). These are system-wide options.
- **Section** = a named division within a class+gender section (e.g., "Section A", "Section B").

#### How to navigate to sections

1. Click **Sections** in the sidebar. You will see all classes.
2. Click a class to see its Gender Sections.
3. Click a Gender Section to manage individual Sections under it.

#### How to add a section

1. Navigate to the desired class and gender section.
2. Click **Add Section**.
3. Enter the section name.
4. Click **Save**.

#### DO NOT

- Do not create a section without first selecting the correct class and gender section — the hierarchy matters.
- Do not delete a section that has students assigned to it.

---

### 3.7 Grading Policies

**Path:** Setup → Grading Policies

A Grading Policy defines a set of grade boundaries (e.g., A+ = 90–100, A = 80–89, etc.). These are used when calculating results.

#### How to add a grading policy

1. Click **Add Policy**.
2. Enter a name for the policy (e.g., "Standard Grade Policy 2024").
3. Click **Save**.

#### How to add grade rows inside a policy

1. Click on the policy name or its expand/edit icon.
2. Inside the policy, click **Add Grade**.
3. Fill in:
   - **Grade Label** (e.g., `A+`, `B`)
   - **Minimum Mark** and **Maximum Mark**
   - **Grade Point** (GPA value)
4. Click **Save**.

#### Active / Inactive policies

- Use the toggle to activate or deactivate a policy.
- Only active policies are available for assignment to marking structures.

#### DO NOT

- Do not create overlapping mark ranges within the same policy (e.g., two grades both covering 80–89).
- Do not deactivate a policy that is currently linked to an active marking structure.
- Do not delete a policy that has been used to compute published results.

---

## 4. Students

---

### 4.1 All Students (Student List)

**Path:** Students → All Students

This is the main student management table showing all **active** student enrollments.

#### Filters

Use the filter panel at the top to narrow down the list:

| Filter | Description |
|---|---|
| Academic Year | Filter by the year of enrollment |
| Shift | Filter by the shift the student belongs to |
| Class | Cascades from Shift — shows only classes in that shift |
| Gender Section | Boys, Girls, or Combined |
| Section | Cascades from Class + Gender Section |
| Student Group | Optional group (Science, Arts, etc.) |

- After selecting your filters, click the **Search** button to apply them.
- Click **Clear** (or reset icon) to remove all filters.

#### Sorting

Click any column header to sort the list by that column. Click again to reverse the sort direction.

Available sort columns: Row Number, Student ID, Name, Class Roll, Parent Info, Date of Birth, Gender.

#### Bulk Actions

1. Check the checkbox in the table header to **select all** visible students.
2. Check individual checkboxes to select specific students.
3. With students selected, use the action buttons that appear (e.g., **Deactivate** / archive selected students).

#### Exporting

A data export option is available for the filtered list. Select the columns you want and download the file.

#### DO NOT

- Do not deactivate a student without confirming — deactivated students move to **Archived Students** and are no longer visible here.
- Do not use sorting while a bulk selection is active — clear your selection first to avoid confusion.

---

### 4.2 Add a New Student

**Path:** Students → All Students → click the **Add Student** button (or the **+** icon)

The student creation form is split into logical sections:

#### Section 1 — Photo

- Click the photo area to **upload a student photo**.
- Supported formats: JPG, PNG.
- The photo appears on the student's ID card and other printed documents.

#### Section 2 — Basic Information

| Field | Required | Notes |
|---|---|---|
| Name (Bangla) | Yes | Student's full name in Bangla script |
| Name (English) | Yes | Student's full name in English |
| Date of Birth | Yes | Click the date field to open a calendar picker |
| Nationality | No | Defaults can be left as-is |
| System Student ID | Yes | Unique identifier for the student in the system |
| Class Roll | Yes | The roll number within the class |
| Gender | Yes | Select from dropdown |

#### Section 3 — Academic Assignment

| Field | Required | Notes |
|---|---|---|
| Academic Year | Yes | The year this enrollment belongs to |
| Shift | Yes | Which shift the student attends |
| Class | Yes | Cascades from Shift |
| Gender Section | Yes | Boys / Girls / Combined |
| Section | No | Cascades from Class + Gender Section |
| Student Group | No | Science, Arts, Commerce, etc. |
| Student Status | No | Active by default |

#### Section 4 — Father's Information

| Field | Notes |
|---|---|
| Father Name (Bangla) | |
| Father Name (English) | |
| Father Phone | Used for contact |
| Father Occupation | |
| Father Monthly Salary | |

#### Section 5 — Mother's Information

Same fields as Father (Name Bangla, Name English, Phone, Occupation, Monthly Salary).

#### Section 6 — Guardian's Information

Same fields as Father/Mother — fill only if guardian is different from parents.

#### Section 7 — Current Address

| Field | Notes |
|---|---|
| Holding | House/holding number |
| Road | Road name or number |
| District | |
| Thana/Upazila | |

#### Section 8 — Permanent Address

Same fields as Current Address. Fill separately if different from current address.

#### Saving the student

- Click **Save** at the bottom of the form.
- A success toast appears if saved correctly.
- The new student is immediately visible in the All Students list.

#### DO NOT

- Do not leave the **System Student ID** blank — it must be unique across all students.
- Do not enter a duplicate **Class Roll** within the same class and section in the same academic year.
- Do not upload a photo larger than 2–3 MB — it may cause the upload to time out.
- Do not submit the form without selecting the Academic Year and Class — these are mandatory for the enrollment record.

---

### 4.3 Edit a Student

**Path:** Students → All Students → click the **Edit** (pencil) icon on a student row

The edit form is identical to the create form but is pre-filled with the student's existing data.

- Make your changes and click **Save**.
- Only the fields you changed will be updated.

#### DO NOT

- Do not change the **System Student ID** to one that belongs to another student.
- Do not change the **Academic Year** of an existing student unless you intend to create a new enrollment (this effectively moves them).

---

### 4.4 View Student Details

**Path:** Students → All Students → click the **View** (eye) icon on a student row

A read-only detail page showing all the student's information:

- Personal info, photo, addresses
- Father, Mother, Guardian info
- Current enrollment details (class, section, shift, group, roll number)

From this page you can also navigate to **Edit** using the edit button.

---

### 4.5 Archived Students

**Path:** Students → Archived Students

Shows all **inactive** (deactivated/archived) student enrollments.

#### How to re-activate students

1. Use the filters (Academic Year, Shift, Class, Gender Section, Section, Group) to find the students.
2. Click **Search**.
3. Select the students you want to re-activate using the checkboxes.
4. Click **Activate** (the button that appears when students are selected).
5. Confirm when prompted.

The re-activated students will reappear in the **All Students** list.

#### DO NOT

- Do not re-activate a student into a class or academic year they no longer belong to without first editing their enrollment details.

---

### 4.6 Student Transfer

**Path:** Students → Student Transfer

Use this page to move students from one section/class to another **within the same system**.

#### How to transfer students

1. In the **Source** panel, select the Academic Year, Shift, Class, Gender Section, Section, and Group of the students you want to transfer.
2. Click **Search** to load the student list.
3. Select the students to transfer using their checkboxes.
4. In the **Destination** panel, select the target Academic Year, Shift, Class, Gender Section, Section, and Group.
5. Click **Transfer**.
6. Confirm the action in the confirmation dialog.

#### DO NOT

- Do not transfer students to a destination section that does not exist — create the section first in Setup → Sections.
- Do not transfer all students from a class without first verifying the destination has capacity.
- Do not cancel mid-transfer — always wait for the success confirmation.

---

### 4.7 Student Migration

**Path:** Students → Student Transfer → (Migration tab or page)

Student Migration is used to promote students from one academic year to the next (e.g., promoting all Class 5 students to Class 6 for the new year).

#### How to migrate students

1. Select the **Source** filters (Academic Year, Shift, Class, Section, Group).
2. Click **Search** to load students.
3. Select the students to migrate.
4. Select the **Target** Academic Year, Class, Section, and Group.
5. Click **Migrate**.
6. Confirm in the dialog.

#### DO NOT

- Do not migrate students before the new academic year has been created in Setup → Academic Years.
- Do not migrate the same students twice into the same year — this creates duplicate enrollments.

---

## 5. Exam

The Exam section must be configured in order: **Rooms → Subjects → Exam Types → Exam Routines → Sessions**.

---

### 5.1 Rooms

**Path:** Exam → Rooms

Rooms are the physical exam halls used during seat plan generation.

#### How to add a room

1. Click **Add Room**.
2. Enter the **Room Name** and **Capacity** (how many students it fits).
3. Click **Save**.

#### DO NOT

- Do not set a room capacity of 0 — it will not be available for seat plan assignment.
- Do not delete a room that is assigned to an active exam session.

---

### 5.2 Subjects

**Path:** Exam → Subjects

Subjects are the courses students are examined in.

#### How to add a subject

1. Click **Add Subject**.
2. Enter the subject name.
3. Click **Save**.

#### DO NOT

- Do not create duplicate subjects with the same name — it causes confusion in marking structures.
- Do not delete a subject that is part of an active marking structure.

---

### 5.3 Exam Types

**Path:** Exam → Exam Types

Exam Types categorize what kind of exam a routine covers (e.g., Half-Yearly, Annual, First Term).

#### How to add an exam type

1. Click **Add Exam Type**.
2. Enter the type name.
3. Click **Save**.

#### Active / Inactive

- Toggle the active switch to control which exam types appear in routine creation dropdowns.

#### DO NOT

- Do not delete an exam type linked to existing routines.
- Do not deactivate an exam type while its routines are still in use.

---

### 5.4 Exam Routines & Sessions

**Path:** Exam → Exam Routines

An **Exam Routine** is a named exam event (e.g., "Annual Exam 2024"). Under each routine you create **Sessions** — each session is one exam sitting (a specific date, time, and subject).

#### How to create a routine

1. Click **Add Routine**.
2. Fill in:
   - **Title** (e.g., `Annual Exam 2024`)
   - **Exam Type** (select from existing types)
   - **Academic Year**
3. Click **Save**.

#### Filtering routines

- Use the **Exam Type** filter at the top to narrow the list.
- Toggle the **Show Inactive** switch to view archived routines.

#### How to manage sessions inside a routine

1. Click on a routine row (or its **Sessions** button).
2. You are taken to the **Exam Sessions** page for that routine.

#### How to add a session

1. Click **Add Session**.
2. Fill in:
   - **Date**
   - **Start Time** and **End Time**
   - **Subject**
   - **Classes** participating in this session
3. Click **Save**.

#### Cloning a routine

- Use the **Clone** button on a routine to copy its full structure (including sessions) into a new routine. This saves time when creating a similar exam for a new year.

#### DO NOT

- Do not create a session without selecting at least one class — it will have no students.
- Do not set a session's end time before its start time.
- Do not delete a session after marks have been entered for it — marks will be lost.
- Do not clone a routine and forget to update the academic year and session dates on the cloned copy.

---

## 6. Marking

The Marking section is where you define how marks are structured and then enter the actual student marks.

**Recommended order:** Exam Components → Marking Structures → Mark Entry

---

### 6.1 Exam Components

**Path:** Marking → Exam Components

Exam Components are the individual parts of a mark (e.g., Written, MCQ, Practical, Viva).

#### How to add an exam component

1. Click **Add Component**.
2. Enter:
   - **Name** (e.g., `Written`, `MCQ`)
   - **Order Index** (determines the display order in mark sheets; lower number = appears first)
3. Click **Save**.

#### DO NOT

- Do not create duplicate component names — it makes marking structures confusing.
- Do not change the order index of a component after marks have been entered.

---

### 6.2 Marking Structures

**Path:** Marking → Marking Structures

A Marking Structure defines the total marks breakdown for a particular combination of **Exam Type + Class + Subject Group + Subject**.

#### How to filter/find structures

Use the filter dropdowns at the top:
- **Exam Type** — e.g., Annual, Half-Yearly
- **Class** — the class the structure applies to
- **Group** — the student subject group (Science, Arts, etc.)

Click **Load** or **Search** to display matching structures.

#### How to add a marking structure

1. Select the Exam Type, Class, and Group in the filters.
2. Click **Add Structure**.
3. In the dialog:
   - Select the **Subject**
   - For each **Exam Component** (Written, MCQ, etc.), enter the **Full Marks** allocated to that component
4. Click **Save**.

#### DO NOT

- Do not create two marking structures for the same Exam Type + Class + Group + Subject combination.
- Do not set the total marks so high that they are unreachable — verify the sum of all components makes sense.
- Do not create a marking structure before the Exam Type, Class, Group, and Subject exist in the system.

---

### 6.3 Mark Entry

**Path:** Marking → Mark Entry

This is where teachers enter student marks for each subject and exam session.

#### How to load the mark sheet

1. Select **Academic Year** from the first dropdown (defaults to the current year).
2. Select **Exam Routine**.
3. Select **Exam Session** (a specific date/subject sitting).
4. Select **Class**.
5. Optionally select **Gender Section**, **Section**, and **Group** to narrow down the student list.
6. Click **Load Sheet** (or the apply button).

The mark sheet table appears with one row per student.

#### How to enter marks

- Each student row has input cells for each exam component (e.g., Written, MCQ).
- Click into a cell and type the mark. Use **Tab** to move to the next cell.
- If a student was **absent**, toggle the **Absent** status for that row — the mark cells will be disabled.
- If a student was **expelled**, toggle the **Expelled** status.

#### How to save marks

- Click **Save Marks** after entering all marks.
- A success toast confirms the save.

#### Printing the blank mark sheet

- Click **Print Sheet** to download a blank PDF mark sheet for offline use.

#### DO NOT

- Do not enter a mark higher than the **Full Marks** defined in the Marking Structure for that component — the system may reject or flag it.
- Do not leave the page without saving — unsaved marks will be lost.
- Do not mark a student as Absent and also enter a mark — pick one.
- Do not enter marks before the Marking Structure for that subject exists.
- Do not load the sheet without selecting the Routine and Session — the list will not appear.

---

## 7. Results

Results are automatically calculated from the marks entered. You do not enter results manually — they are derived.

---

### 7.1 Session Results

**Path:** Results → Session Results

Shows the results for a **single exam session** (one sitting, one subject on one date).

#### How to view

1. Select **Academic Year**.
2. Select **Exam Routine**.
3. Select **Exam Session**.
4. Select **Class**.
5. Optionally select **Subject** to narrow further.
6. Click **Load**.

The result table shows each student's marks, grade, and pass/fail status for that session.

#### Downloading

- Use the **Download PDF** button to export the session result as a PDF.

#### DO NOT

- Do not interpret session results as final results — they only cover one subject sitting.

---

### 7.2 Routine Results

**Path:** Results → Routine Results

Shows the combined results across **all sessions** within an exam routine for a class.

#### How to view

1. Select **Academic Year**.
2. Select **Exam Routine**.
3. Select **Class**.
4. Click **Load**.

A table appears with each student's subject-wise marks and totals, along with grade and position.

#### Downloading

- Click **Download PDF** to generate a PDF tabulation sheet.

#### DO NOT

- Do not view routine results until all session marks for the routine have been saved — incomplete marks will give incorrect totals.

---

### 7.3 Annual Results

**Path:** Results → Annual Results

Shows the aggregated results across **all routines** in the academic year for a class (e.g., combining Half-Yearly + Annual).

#### How to view

1. Select **Academic Year**.
2. Select **Class**.
3. Click **Load**.

#### Downloading

- Click **Download PDF** to generate the annual result sheet.

---

### 7.4 Merit List

**Path:** Results → Merit List

Shows students ranked by their total marks or GPA within a class/section.

#### How to view

1. Select **Academic Year**, **Shift**, **Class**.
2. Optionally filter by **Gender Section**, **Section**, **Group**.
3. Click **Load**.

Students are displayed in ranked order. The top 3 positions are highlighted with gold, silver, and bronze indicators.

#### DO NOT

- Do not publish the merit list until all marks for all subjects have been entered and saved.

---

### 7.5 Statistics

**Path:** Results → Statistics

Displays statistical summaries — pass rate, fail count, subject-wise performance, grade distribution — for a selected routine or session.

#### How to use

1. Select the relevant filters (Academic Year, Routine, Class).
2. Click **Load**.
3. View the bar charts and summary cards.

---

## 8. Downloads

All download pages generate PDFs. The browser will either open the PDF in a new tab or trigger a file download automatically, depending on your browser settings.

---

### 8.1 Student ID Card

**Path:** Downloads → Student ID Card

Generates ID cards for students. Each card includes the student's photo, name, class, roll number, and institution logo.

#### How to generate

1. Select **Academic Year**, **Shift**, **Class**.
2. Optionally narrow by **Gender Section**, **Section**, **Group**.
3. Click **Generate / Download**.

The PDF contains all matching students, with one or more ID cards per page.

#### DO NOT

- Do not generate ID cards for students who do not yet have a photo uploaded — their photo area will appear blank.
- Do not generate ID cards before setting the institution logo in System Settings.

---

### 8.2 Admit Card

**Path:** Downloads → Admit Card

Generates exam admit cards. Each card shows the student's name, roll number, class, and the full exam schedule (dates, times, subjects) from the selected routine.

#### How to generate

1. Select the **Exam Routine**.
2. Select **Class**.
3. Optionally filter by **Gender Section**, **Section**, **Group**.
4. Optionally enter a **Roll Range** (Start Roll – End Roll) to generate for a subset of students only.
5. Click **Download**.

#### DO NOT

- Do not generate admit cards for a routine that has no sessions — the schedule section will be empty.
- Do not generate admit cards before the routine's exam sessions are fully configured.
- Do not use roll range filters unless you specifically need a subset — leave both fields empty to get all students.

---

### 8.3 Progress Report

**Path:** Downloads → Progress Report

Generates individual progress report cards per student, showing their marks and grades across all subjects in a selected routine.

#### How to generate

1. Select the **Exam Routine**.
2. Select **Shift**, **Class**.
3. Optionally filter by **Gender Section**, **Section**, **Group**.
4. Optionally enter a **Roll Range** to limit the output.
5. Click **Download**.

#### DO NOT

- Do not generate progress reports before all marks for the routine have been saved — incomplete reports will show blank cells.
- Do not generate for a very large class all at once if the file is too large; use the roll range filter to break it into batches.

---

### 8.4 Seat Plan

**Path:** Downloads → Seat Plan

Generates a printed seat plan assigning students to specific rooms for an exam.

#### How to generate

1. Select the **Exam Routine**.
2. Select **Shift**, **Class**.
3. Optionally filter by **Section** or **Group**.
4. Click **Generate / Download**.

The PDF shows room-wise seating assignments.

#### DO NOT

- Do not generate a seat plan before rooms have been created and configured in Exam → Rooms.
- Do not generate if the routine has no sessions — the system will have nothing to plan around.

---

## 9. Administration

**Path:** Administration → Admins

This page is reserved for managing admin user accounts. It is currently under development. Do not attempt to use it for any operational task.

---

## 10. Common Rules & Warnings

### Data Entry Order

The system has a strict data dependency chain. Setting up out of order will result in empty dropdowns and errors:

```
System Settings
    ↓
Academic Years
    ↓
Shifts → Classes → Groups → Sections
    ↓
Students (enroll with above data)
    ↓
Exam Types → Subjects → Rooms
    ↓
Exam Routines → Sessions
    ↓
Exam Components → Marking Structures
    ↓
Mark Entry
    ↓
Results & Downloads
```

Follow this order strictly when setting up for a new academic year.

---

### General DO NOTs (Global)

- **Do not delete master data** (Shifts, Classes, Sections, Subjects, Exam Types) once students or exam results are linked to them. Deletion cascades and can destroy records silently.
- **Do not duplicate** academic years, subjects, exam types, or grading policies — duplicates cause confusion in filter dropdowns throughout the system.
- **Do not navigate away** from a form while it is saving (a loading spinner is visible). Wait for the success toast first.
- **Do not use the browser's Back button** to return to a previous page — always use the sidebar navigation. The browser back button can cause stale data to appear.
- **Do not open the same page in two browser tabs simultaneously** while entering marks or saving student data — the two tabs can overwrite each other.
- **Do not refresh the page** while a file download is in progress — it may cancel the download.

---

### Understanding Status Indicators

| Color / Icon | Meaning |
|---|---|
| Blue highlight in sidebar | Currently active page |
| Green toast at top-right | Action succeeded |
| Red toast at top-right | Action failed — read the message |
| Loading spinner | Operation in progress — do not click away |
| Toggle switch (blue) | Item is Active |
| Toggle switch (grey) | Item is Inactive |

---

### Filters and Search

- Most list pages have a **pending filter** system: you must click **Search** after setting filters for them to take effect. Changing a filter dropdown alone does not reload the data.
- Filters cascade from top to bottom — selecting a higher-level filter (e.g., Shift) clears the dependent lower-level filters (Class, Section) automatically.

---

*End of User Manual*
