# E_Shop Task Management System Documentation

## Overview
The Task Management System is a comprehensive feature integrated into the E_Shop application that allows users to create, assign, track, and manage tasks. This system is particularly useful for admin users to manage order processing, inventory tasks, customer service requests, and other operational activities.

## Features

### Core Functionality
- ✅ Create and manage tasks
- ✅ Assign tasks to users
- ✅ Track task status and priority
- ✅ Set due dates and deadlines
- ✅ Search and filter tasks
- ✅ Task statistics and reporting
- ✅ Overdue task notifications
- ✅ Task completion tracking

### Task Statuses
- **PENDING**: Task created but not started
- **IN_PROGRESS**: Task is currently being worked on
- **COMPLETED**: Task finished successfully
- **CANCELLED**: Task cancelled or no longer needed

### Task Priorities
- **LOW**: Non-urgent tasks
- **MEDIUM**: Standard priority tasks
- **HIGH**: Important tasks requiring attention
- **URGENT**: Critical tasks needing immediate action

## API Endpoints

### Base URL: `/api/v1/tasks`

### Authentication
All endpoints require Bearer token authentication:
```
Authorization: Bearer <access_token>
```

---

## 📋 Task Management Endpoints

### 1. Create Task
**POST** `/api/v1/tasks`

**Request Body:**
```json
{
  "title": "Process Order #12345",
  "description": "Review and process customer order for iPhone 15",
  "priority": "HIGH",
  "assignedUserId": 2,
  "dueDate": "2026-05-10T17:00:00"
}
```

**Response:**
```json
{
  "message": "Success",
  "code": "200",
  "data": {
    "id": 1,
    "title": "Process Order #12345",
    "description": "Review and process customer order for iPhone 15",
    "status": "PENDING",
    "priority": "HIGH",
    "assignedUserId": 2,
    "assignedUserName": "John Doe",
    "createdByUserId": 1,
    "createdByUserName": "Admin User",
    "dueDate": "2026-05-10T17:00:00",
    "createdAt": "2026-05-04T10:30:00",
    "updatedAt": "2026-05-04T10:30:00"
  }
}
```

---

### 2. Get All Tasks
**GET** `/api/v1/tasks?page=0&size=10&sort=id,desc`

**Response:**
```json
{
  "message": "Success",
  "code": "200",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Process Order #12345",
        "status": "PENDING",
        "priority": "HIGH",
        "assignedUserName": "John Doe",
        "dueDate": "2026-05-10T17:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10,
      "sort": "id,desc"
    },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### 3. Get Task by ID
**GET** `/api/v1/tasks/{id}`

**Response:** Same as create task response

---

### 4. Update Task
**PUT** `/api/v1/tasks/{id}`

**Request Body:** Same as create task

---

### 5. Update Task Status
**PATCH** `/api/v1/tasks/{id}/status?status=IN_PROGRESS`

**Valid Status Values:** PENDING, IN_PROGRESS, COMPLETED, CANCELLED

---

### 6. Assign Task to User
**POST** `/api/v1/tasks/{id}/assign?userId=2`

---

### 7. Get Tasks by Assigned User
**GET** `/api/v1/tasks/user/{userId}?page=0&size=10`

---

### 8. Get Tasks by Status
**GET** `/api/v1/tasks/status/{status}?page=0&size=10`

**Status Values:** PENDING, IN_PROGRESS, COMPLETED, CANCELLED

---

### 9. Get Tasks Created by User
**GET** `/api/v1/tasks/created-by/{userId}?page=0&size=10`

---

### 10. Search Tasks
**GET** `/api/v1/tasks/search?keyword=order&page=0&size=10`

---

### 11. Get Overdue Tasks
**GET** `/api/v1/tasks/overdue`

**Response:**
```json
{
  "message": "Success",
  "code": "200",
  "data": [
    {
      "id": 1,
      "title": "Overdue Task",
      "dueDate": "2026-05-03T10:00:00",
      "status": "PENDING"
    }
  ]
}
```

---

### 12. Get Tasks Due Today
**GET** `/api/v1/tasks/due-today`

---

### 13. Get Task Statistics
**GET** `/api/v1/tasks/statistics`

**Response:**
```json
{
  "message": "Success",
  "code": "200",
  "data": {
    "pending_count": 5,
    "in_progress_count": 3,
    "completed_count": 12,
    "cancelled_count": 1,
    "total_tasks": 21,
    "overdue_tasks": 2,
    "due_today_tasks": 3
  }
}
```

---

### 14. Delete Task
**DELETE** `/api/v1/tasks/{id}`

---

## 🔐 Security & Permissions

### Role-Based Access
- **ADMIN**: Full access to all task operations
- **USER**: Can only view and update their assigned tasks

### Authentication Required
All endpoints require valid JWT token in Authorization header

---

## 📊 Database Schema

### Task Table (`tbl_task`)
```sql
CREATE TABLE tbl_task (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    assigned_user_id BIGINT REFERENCES tbl_user(id),
    created_by_user_id BIGINT REFERENCES tbl_user(id),
    due_date TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);
```

---

## 🚀 Usage Examples

### Admin Workflow
1. **Create Task**: Admin creates task for order processing
2. **Assign Task**: Assign to specific employee
3. **Monitor Progress**: Track status updates
4. **Review Completion**: Mark as completed when done

### Employee Workflow
1. **View Assigned Tasks**: See all tasks assigned to them
2. **Update Status**: Change from PENDING to IN_PROGRESS
3. **Complete Tasks**: Mark as COMPLETED when finished

---

## 📱 Integration with E_Shop

### Order Processing Tasks
```json
{
  "title": "Process Order #ORD-12345",
  "description": "Customer ordered iPhone 15 Pro - verify payment and ship",
  "priority": "HIGH",
  "assignedUserId": 3,
  "dueDate": "2026-05-05T16:00:00"
}
```

### Inventory Management Tasks
```json
{
  "title": "Restock iPhone 15 Inventory",
  "description": "Current stock: 5 units. Reorder threshold: 10 units",
  "priority": "MEDIUM",
  "assignedUserId": 4,
  "dueDate": "2026-05-07T09:00:00"
}
```

### Customer Service Tasks
```json
{
  "title": "Handle Customer Complaint #CS-789",
  "description": "Customer reported damaged package - investigate and resolve",
  "priority": "URGENT",
  "assignedUserId": 2,
  "dueDate": "2026-05-04T14:00:00"
}
```

---

## 🔧 Configuration

### Application Properties
No additional configuration required - uses existing database and security setup.

### Dependencies
- Spring Data JPA (existing)
- Spring Security (existing)
- PostgreSQL (existing)

---

## 📈 Monitoring & Analytics

### Task Statistics
- Total tasks count
- Tasks by status
- Overdue tasks count
- Tasks due today
- User workload distribution

### Performance Metrics
- Task completion rate
- Average task duration
- Overdue task percentage
- User productivity metrics

---

## 🛠️ Error Handling

### Common Error Responses
```json
{
  "message": "Task not found",
  "code": "404",
  "data": {}
}
```

```json
{
  "message": "User not found",
  "code": "404",
  "data": {}
}
```

```json
{
  "message": "Task title is required",
  "code": "400",
  "data": {}
}
```

---

## 🔄 Future Enhancements

### Planned Features
- [ ] Task comments and attachments
- [ ] Task templates for common operations
- [ ] Email notifications for task assignments
- [ ] Task dependencies and workflows
- [ ] Time tracking and reporting
- [ ] Task categories and tags
- [ ] Recurring tasks
- [ ] Task delegation
- [ ] Mobile app notifications

---

## 📞 Support

For technical support or questions about the Task Management System:
- Check API documentation at `/swagger-ui.html`
- Review application logs for error details
- Contact development team for custom requirements

---

*Last Updated: May 4, 2026*
*Version: 1.0.0*
