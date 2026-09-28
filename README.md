# MockServer – Desktop GUI Mock Server for API Testing

**MockServer** is a free, cross-platform desktop application that lets developers create local HTTP mock servers with a graphical interface. It helps test frontend apps, mobile apps, and backend integrations without needing a real API.

Run lightweight **mock servers** on your machine so you can develop and test clients without hitting a real backend. Need a **dummy server** or standalone HTTP endpoint? Mock Server lets you define routes, status codes, headers, cookies, and delays—then start and stop servers from a simple JavaFX GUI.

---

## Table of contents

- [About Mock Server](#about-mock-server)
- [Who is this for?](#who-is-this-for)
- [Use Cases](#use-cases)
- [Features](#features)
- [Download](#download)
- [FAQ](#faq)
- [How to use Mock Server](#how-to-use-mock-server)
- [Donate](#donate)

---

## About Mock Server

The **Mock Server** application helps you test software locally by creating servers that imitate a real API. You can run **multiple mock servers on different ports** at the same time. Servers are grouped into **collections**; you can **export** a collection and share it with teammates so everyone uses the same **dummy server** definitions—useful for collaboration and consistent integration tests.

The embedded HTTP layer is based on `com.sun.net.httpserver.HttpServer`. For details on that API, see the Java documentation [here](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html).

---

## Who is this for?

- Developers who want a **local mock server** or **dummy server** for HTTP testing  
- Teams that need **shareable mock API** configurations (export/import)  
- Anyone building or testing clients against **custom endpoints**, **status codes**, **headers**, and **cookies** without deploying infrastructure  

---

## Use Cases

- Frontend development without backend ready
- Mobile app API simulation
- QA testing responses
- Local REST API mocking
- Demo environments

---

## Features

| Topic | What you get |
|--------|----------------|
| **Mock server** setup | Name, port, path, method, HTTP status, delay, body |
| **Dummy server** workflows | Start/stop per server or manage all active servers in one view |
| **Organization** | Collections to group mock servers |
| **Collaboration** | Export/import collections (JSON) for backup and sharing |
| **Platform** | Desktop GUI (JavaFX); Available for Windows. macOS release is planned |

- GUI based mock server
- Local REST API mocking
- Local HTTP endpoints
- Custom JSON/XML/Text/Binary responses
- Request inspection
- Frontend/backend/mobile testing
- JavaFX desktop tool

---

## Download

Currently available for **Windows**. A **macOS** release is planned.

- [**Windows**](https://github.com/Kazitanvirazad/mock-server/releases)

---

## FAQ

### What is a mock server?

A **mock server** is a local or test HTTP server that returns predefined responses so you can develop and test clients without a real backend. **Mock Server** (this app) is a desktop tool to configure and run such servers on your machine.

### What is a dummy server?

A **dummy server** is another name for a simple standalone server used in testing. In this project, **Mock Server** acts as a **dummy server** you configure with endpoints, status codes, headers, and cookies.

### Is Mock Server the same as a production API?

No. **Mock Server** is for **local testing** and **integration tests**. It does not replace a real API in production.

### Can I run multiple mock servers at once?

Yes. You can run **multiple mock servers on multiple ports** in parallel, organized by collections.

### How do I share mock server configs with my team?

Use **File → Export Collections** to export JSON, then **File → Import Collections** on another machine. That way teammates can reuse the same **mock server** definitions.

### Which platforms are supported?

**Windows** builds are available. **macOS** support is planned.

---

## How to use Mock Server

After you download Mock Server, install it and open the application. The main window looks like this:

<p align="left">
  <img src="Docs/Main Application.png" alt="Mock Server main application window for managing mock servers and collections" />
</p>

You can create **multiple collections** to organize servers. Each collection can contain **multiple mock servers**.

Go to **Options → Create Collection** or click **Add** to create a collection.

<p align="left">
  <img src="Docs/Create Collection.png" alt="Mock Server create collection dialog" />
</p>

In the Create Collection window, enter the collection name and save it.

<p align="left">
  <img src="Docs/Enter Collection Name.png" alt="Enter collection name in Mock Server" />
</p>

After the collection exists, you can create servers. Click **Create** to open the server form.

<p align="left">
  <img src="Docs/Create Server button.png" alt="Create server button in Mock Server" />
</p>

A new server form opens. **Server Name** and **Server port** are required. Other fields use defaults if left empty.

Default values:

- **Endpoint:** `/`
- **HTTP status code:** `200`
- **Method:** `GET`
- **Delay:** `0` ms
- **Text response:** `0` bytes

<p align="left">
  <img src="Docs/Enter server details.png" alt="Enter mock server details in Mock Server form" />
</p>

Click **Add** at the top-right of the **Header** table to add headers.

<p align="left">
  <img src="Docs/Add header.png" alt="Add HTTP header to mock server in Mock Server" />
</p>

Enter header values and save. To remove a header, use the red trash icon on the row or press **Delete** on Windows.

<p align="left">
  <img src="Docs/Enter header values.png" alt="Enter HTTP header values for dummy server response" />
</p>

Click **Add** at the top-right of the **Cookie** table to add cookies.

<p align="left">
  <img src="Docs/Add Cookie.png" alt="Add cookie to mock server in Mock Server" />
</p>

Enter cookie values and save. To remove a cookie, use the red trash icon or press **Delete** on Windows.

<p align="left">
  <img src="Docs/Enter Cookie Details.png" alt="Enter cookie details for Mock Server response" />
</p>

You can choose which **collection** stores the server from the dropdown.

<p align="left">
  <img src="Docs/Select Collection.png" alt="Select collection for mock server in Mock Server" />
</p>

With all fields filled in, the form looks like this:

<p align="left">
  <img src="Docs/Server all data entered.png" alt="Mock Server form with all server fields completed" />
</p>

Click **Create Server** to save.

<p align="left">
  <img src="Docs/Create Server.png" alt="Create Server button to save mock server configuration" />
</p>

Select the server and click **Start Server** (bottom right) to run your **local mock server**.

<p align="left">
  <img src="Docs/Start Server.png" alt="Start mock server in Mock Server application" />
</p>

Check the **Status** column in the Servers table to confirm the server is active.

<p align="left">
  <img src="Docs/Server Active Status.png" alt="Mock Server status column showing active dummy server" />
</p>

Call the endpoint from a client or browser to receive the configured response.

<p align="left">
  <img src="Docs/invoke mock api.png" alt="Invoke mock API endpoint served by Mock Server" />
</p>

To stop a running server, select it and click **Stop Server**.

<p align="left">
  <img src="Docs/Stop Server.png" alt="Stop mock server in Mock Server" />
</p>

After stopping, the **Status** column reflects the inactive state.

<p align="left">
  <img src="Docs/Server InActive Status.png" alt="Mock Server status showing inactive server" />
</p>

Use **Active Servers** to see every running mock server, stop all at once, or stop selected servers. The section shows how many servers are active.

Click **Active Servers** to open the view:

<p align="left">
  <img src="Docs/Active Servers Section.png" alt="Active Servers section in Mock Server" />
</p>

Or go to **Options → Active Servers**:

<p align="left">
  <img src="Docs/Select Active Servers.png" alt="Options menu Active Servers in Mock Server" />
</p>

From this window you can stop all active **mock servers** in one action.

<p align="left">
  <img src="Docs/Stop All Servers.png" alt="Stop all active mock servers at once" />
</p>

To edit a server, **double-click** the row or press **Enter** on Windows.

<p align="left">
  <img src="Docs/Edit Server.png" alt="Edit mock server in Mock Server" />
</p>

To delete a server, use the red trash icon on the row or press **Delete** on Windows.

<p align="left">
  <img src="Docs/Delete Server.png" alt="Delete mock server row in Mock Server" />
</p>

To edit a collection, **double-click** the row or press **Enter** on Windows.

<p align="left">
  <img src="Docs/Edit Collection.png" alt="Edit collection in Mock Server" />
</p>

To delete a collection, use the red trash icon or press **Delete** on Windows.

<p align="left">
  <img src="Docs/Delete Collection.png" alt="Delete collection in Mock Server" />
</p>

### Export collections

Go to **File → Export Collections**.

<p align="left">
  <img src="Docs/Select Export Collections.png" alt="File menu Export Collections in Mock Server" />
</p>

Choose **Export all** for a full backup or pick a single collection from the dropdown. Select the export directory and click **Export Collection**. Collections export as one JSON file; that file can be **imported** so you can share **mock server** setups with your team.

**Note:** If a server returns content from a **file**, path-only data is stored. Paths may differ on other machines—avoid relying on exported file paths when sharing collections across computers.

<p align="left" style="display: flex; gap: 2px;">
  <img src="Docs/Export Collection options.png" alt="Mock Server export collection options" />
  <img src="Docs/Export Collection path.png" alt="Choose export directory for Mock Server collections" />
</p>
<p align="left" style="display: flex; gap: 2px;">
  <img src="Docs/Exported collection file.png" style="height: 10%; max-width: 40%; aspect-ratio: auto;" alt="Exported Mock Server collection JSON file" />
  <img src="Docs/Collection exported file content.png" style="width: 60%; height: auto;" alt="JSON content of exported mock server collection" />
</p>

### Import collections

Go to **File → Import Collections**.

<p align="left">
  <img src="Docs/Select Import Collections.png" alt="File menu Import Collections in Mock Server" />
</p>

Drag and drop a file or use **Select file**, then click **Import Collection**. Imported collections appear in the collection table. If a collection name already exists, a random suffix is appended.

<p style="display: flex; flex-wrap: wrap; gap: 2px; flex: 50%;">
  <img src="Docs/Import Collections options.png" style="max-width: 50%; height: 10%;" alt="Import collections options in Mock Server" />
  <img src="Docs/Drag n drop.png" style="max-width: 50%; height: 10%;" alt="Drag and drop collection file to import into Mock Server" />
  <img src="Docs/Import Collection path.png" style="max-width: 50%; height: 10%; margin-top: 2px;" alt="Select import file path for Mock Server collections" />
  <img src="Docs/After Import Collection.png" style="max-width: 50%; height: 10%; margin-top: 2px;" alt="Collections table after importing mock server data" />
</p>

### Settings

Go to **File → Settings**.

<p align="left">
  <img src="Docs/Select Setting.png" alt="File menu Settings in Mock Server" />
</p>

Choose whether **active mock servers** should **restart automatically** when you reopen Mock Server after closing the app.

<p align="left">
  <img src="Docs/Settings.png" alt="Mock Server settings for auto-restarting active servers" />
</p>

---

## Donate

If you find **Mock Server** useful, consider buying me a coffee.

**UPI QR Scan:**<br>
<p align="left">
  <img src="Docs/scannpay.jpg" style="width: 400px; aspect-ratio: auto;" alt="Scan UPI QR code to Donate me" />
</p>

**Bank Transfer Details:**<br>
**Account name**- Kazi tanvir azad<br>
**Account Number**- 50100462520399<br>
**IFSC**- HDFC0000833

---

*Repository: open-source **Mock Server** — JavaFX GUI for **local mock servers** and **dummy server** testing.*
