# Project Overview

This project involves implementing and integrating Slack messaging functionality, email services, and scheduled tasks for the system. Below are the details of the files I worked on and their specific purposes.

## Features Implemented
1. **Sync Tasks**: Implemented a scheduled task (`SyncDbTask`) to synchronize client preferences and other fields between databases.
2. **Messaging Service**: Created a service (`MsgSlackTask`) that handles sending Slack messages based on client settings and message status.
3. **Mail Service Enhancements**: Modified the `MailService` class to include Slack messaging logic and enhanced email sending capabilities.
4. **Database Mapper Updates**: Updated mapper files (`PmsClientMapper.xml` and `UserMapper.java`) to include necessary fields for client synchronization.

## Files and Descriptions

### 1. `MailService.java`
- **Description**: This file contains the logic for sending emails and Slack messages. I added the functionality for sending Slack notifications alongside email notifications based on client preferences and the message status.
- **Key Changes**:
  - Added methods to integrate Slack messaging.
  - Enhanced email-sending capabilities by including templates for different message types.

### 2. `MsgSlackTask.java`
- **Description**: This file defines a scheduled task responsible for processing and sending Slack messages. It checks the message status (e.g., `TO_BE_SENT`) and sends messages accordingly.
- **Key Changes**:
  - Created a task to automate Slack messaging and ensure consistent communication.

### 3. `SyncDbTask.java`
- **Description**: This file contains the logic for synchronizing database fields, such as `pref_lang` and other client settings, between different databases on a scheduled basis.
- **Key Changes**:
  - Implemented logic to check for updates and synchronize fields periodically.

### 4. `PmsClientMapper.xml`
- **Description**: This XML file is used for mapping database queries related to the `PmsClient` table. I added new fields that need to be synchronized during the sync task.
- **Key Changes**:
  - Added mappings for `pref_lang` and other necessary fields.

### 5. `UserMapper.java`
- **Description**: This file contains database mappings for user-related operations. I updated this file to include the necessary fields for syncing user preferences and settings.
- **Key Changes**:
  - Added methods for retrieving and updating fields such as `pref_lang`.

### 6. `SlackService.java`
- **Description**: This file defines a service that interacts with the Slack API to send messages and attachments to specific channels. It provides methods for sending messages and uploading files as attachments.
- **Key Changes**:
  - Implemented methods for sending Slack messages with or without attachments.
