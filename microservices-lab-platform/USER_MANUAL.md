# 📘 User Manual: Lab Equipment Booking Platform

Welcome to the **University Lab Equipment Booking & Maintenance Platform**. This guide provides step-by-step instructions on how to use the system as both a Student (User) and a Lab Manager (Admin).

---

## 📋 Table of Contents
1. [Getting Started](#1-getting-started)
2. [Authentication & Roles](#2-authentication--roles)
3. [For Students: Browsing & Booking](#3-for-students-browsing--booking)
4. [For Admins: Managing Inventory](#4-for-admins-managing-inventory)
5. [Monitoring System Health](#5-monitoring-system-health)
6. [Troubleshooting](#6-troubleshooting)

---

## 1. Getting Started

### 1.1 Accessing the Platform
The platform is accessible via your web browser. 
1. Open your browser and navigate to the `frontend/index.html` file provided by your system administrator, or the URL where it is hosted (e.g., `http://localhost:3000`).
2. You will land on the **🏠 Dashboard**. Here you can see an overview of the system architecture and current statistics (Total Equipment, Total Bookings, Available Items).

---

## 2. Authentication & Roles

The system uses role-based access. Your capabilities depend on whether you log in as a **USER** (Student/Researcher) or an **ADMIN** (Lab Manager).

### 2.1 Logging In
1. Click on the **🔑 JWT Auth** tab in the top navigation bar.
2. Under "Login Panel", choose your login type:
   - **Login as USER:** (Username: `user1`, Password: `user123`) — Grants standard access to view equipment and create bookings.
   - **Login as ADMIN:** (Username: `admin`, Password: `admin123`) — Grants full access to add/remove equipment and cancel any bookings.
3. Once clicked, a green success message will appear, and your secure token will be saved to your browser automatically.

> [!IMPORTANT]
> You must log in before you can interact with the Equipment or Bookings tabs. If you try to perform actions without logging in, the system will reject your request with a `401 Unauthorized` error.

### 2.2 Role Capabilities
- **Users can:** View all equipment, check availability, create bookings for themselves, and view their own bookings.
- **Admins can:** Do everything a user can, PLUS create new equipment, update equipment details, delete equipment, view all users' bookings, and cancel bookings.

---

## 3. For Students: Browsing & Booking

### 3.1 Viewing Available Equipment
1. Navigate to the **🔬 Equipment** tab.
2. Click the **"🔄 Refresh"** button to load the latest inventory from the database.
3. The table will display all lab equipment, including its Category (e.g., ELECTRONICS, COMPUTING), Location, and Current Status (`AVAILABLE`, `BOOKED`, or `MAINTENANCE`).

### 3.2 Checking Equipment Availability
1. Find the equipment you want in the list.
2. Click the **"Avail."** button next to it.
3. A pop-up will confirm if the equipment is currently available or if it requires maintenance.

### 3.3 Making a Booking
1. Note the **ID** of the equipment you wish to book from the Equipment tab.
2. Navigate to the **📅 Bookings** tab.
3. In the "Create Booking" section:
   - Enter the **Equipment ID**.
   - Click **"Set Times"** to automatically fill in a valid 2-hour window starting from the current time.
   - (Optional) Add notes, such as the purpose of your experiment.
4. Click **"Create Booking → 201"**.
5. A green success box will appear showing your Confirmed Booking ID.

> [!NOTE]
> If someone else has already booked the equipment for the same time window, the system will prevent double-booking and return a `409 Conflict` error.

---

## 4. For Admins: Managing Inventory

As an administrator, you are responsible for maintaining the lab equipment catalog. Ensure you are logged in as **ADMIN** in the Auth tab.

### 4.1 Adding New Equipment
1. Navigate to the **🔬 Equipment** tab.
2. Under the "Create Equipment" panel, you can manually type in the details or use the quick presets:
   - Click **"Prefill Oscilloscope"** or **"Prefill FPGA"** to auto-populate standard lab items.
3. Click **"Create Equipment"**. 
4. The item is immediately added to the catalog with an `AVAILABLE` status.

### 4.2 Removing Equipment
If equipment is broken or retired:
1. Find the equipment in the inventory table.
2. Click the **"Delete"** button next to it.
3. The system will permanently remove it from the catalog.

### 4.3 Understanding the Maintenance Cycle
Every time an item is booked, its `usageCount` automatically increases. 
- Once an item is booked **5 times**, the system automatically flags it for maintenance. 
- Its status changes, and it can no longer be booked by students until an Admin inspects it and clears the maintenance flag via the backend API.

### 4.4 Cancelling Bookings
1. Navigate to the **📅 Bookings** tab.
2. Find the booking ID in the list.
3. Type the ID into the "Cancel Booking" input box and click **"Cancel Booking"**.

---

## 5. Monitoring System Health

The platform is designed to be highly observable for IT support and Admins.

### 5.1 Checking Service Health
1. Go to the **📡 Observability** tab.
2. Click **"Check All"** under the Health Endpoints section.
3. The system will ping the Gateway, Booking Service, Equipment Service, and Config Server. All dots should turn Green.

### 5.2 Tracing a Request
Every action you take in the system generates a unique "Trace ID".
- When you create a booking or view equipment, look at the Response panel. You will see a `traceId` (e.g., `a1b2c3d4e5f6...`).
- IT staff can enter this Trace ID into **Zipkin** (accessible via the `http://localhost:9411` link) to track exactly how the network handled your request.

---

## 6. Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| **Buttons do nothing / UI feels stuck** | Services might not be running. | Go to the Dashboard tab and click the **Ping API Gateway** button. If it turns red, contact IT to start the Docker containers. |
| **Error: 401 Unauthorized** | Your login session expired. | Go to the JWT Auth tab and click **Login as USER** or **ADMIN** again. |
| **Error: 403 Forbidden** | You are logged in as a USER trying to do an ADMIN action. | Log in as an Admin. |
| **Error: 503 Service Unavailable** | The specific backend service is down. | The system's circuit breaker has engaged to protect itself. Wait 30 seconds and try again, or contact IT. |
| **Error: 400 Bad Request** | The dates you entered for a booking are invalid. | Ensure your booking **Start Time** is in the future, and the **End Time** is after the Start Time. Click "Set Times" to fix automatically. |
