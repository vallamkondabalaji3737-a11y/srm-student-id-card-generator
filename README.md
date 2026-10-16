# SRM AP Student ID Card Generator

This folder contains your sister's original front-end files with an SRM email entry screen and a Java backend that saves student details locally.

## Open in VS Code

1. In VS Code, choose **File → Open Folder**.
2. Select this `srm-student-id-card` folder.
3. In the VS Code terminal, compile and start the backend: `javac Server.java` then `java Server`.
4. Open `http://localhost:8080` in your browser.
5. Enter a valid email address whose domain is exactly `@srmap.edu.in`, then select **Enter**.

The same domain rule is checked by the Java backend before it saves a record. Student details are stored in `data/students.jsonl`. The uploaded photo remains in the browser. This checks the address format and domain but does not verify that the user owns the mailbox; mailbox verification would require an email code or link.

API routes: `GET /api/health`, `GET /api/students`, and `POST /api/students`.
