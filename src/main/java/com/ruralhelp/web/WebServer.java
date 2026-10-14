package com.ruralhelp.web;

import com.ruralhelp.dao.StatusHistoryDAO;
import com.ruralhelp.model.*;
import com.ruralhelp.service.*;
import com.ruralhelp.util.DateTimeUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in HTTP Web Server serving the Rural Community Support Platform web portal and REST APIs.
 */
public class WebServer {
    private static final int DEFAULT_PORT = 8080;

    private final AuthService authService = new AuthService();
    private final IssueService issueService = new IssueService();
    private final MatchingService matchingService = new MatchingService();
    private final NgoService ngoService = new NgoService();
    private final ReportService reportService = new ReportService();
    private final StatusHistoryDAO statusHistoryDAO = new StatusHistoryDAO();

    private HttpServer server;
    private int port;
    private final Map<String, Integer> adminSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        }
        WebServer app = new WebServer();
        try {
            app.start(port);
            System.out.println("==================================================================");
            System.out.println("  Rural Community Support Platform - Web Portal is RUNNING!       ");
            System.out.println("  URL: http://localhost:" + app.getPort() + "                    ");
            System.out.println("==================================================================");

            // Open browser
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + app.getPort()));
            } catch (Exception e) {
                System.out.println("Please open http://localhost:" + app.getPort() + " in your browser.");
            }

        } catch (Exception e) {
            System.err.println("Failed to start web server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public void start(int preferredPort) throws IOException {
        this.port = preferredPort;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Contexts
        server.createContext("/api/login", this::handleLogin);
        server.createContext("/api/admin-login", this::handleAdminLogin);
        server.createContext("/api/admin-logout", this::handleAdminLogout);
        server.createContext("/api/issues", this::handleIssues);
        server.createContext("/api/verify", this::handleVerify);
        server.createContext("/api/reject", this::handleReject);
        server.createContext("/api/close", this::handleClose);
        server.createContext("/api/history", this::handleHistory);
        server.createContext("/api/matching", this::handleMatching);
        server.createContext("/api/allocate", this::handleAllocate);
        server.createContext("/api/ngo/tasks", this::handleNgoTasks);
        server.createContext("/api/ngo/status", this::handleNgoStatus);
        server.createContext("/api/dashboard", this::handleDashboard);
        server.createContext("/api/ngos", this::handleNgos);
        server.createContext("/api/register/member", this::handleRegisterMember);
        server.createContext("/api/register/ngo", this::handleRegisterNgo);

        // Static Web Portal at root
        server.createContext("/", this::handleStaticPortal);

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // API HANDLERS
    // =========================================================================

    private void handleLogin(HttpExchange exchange) throws IOException {
        authenticateLogin(exchange, false);
    }

    private void handleAdminLogin(HttpExchange exchange) throws IOException {
        authenticateLogin(exchange, true);
    }

    private void authenticateLogin(HttpExchange exchange, boolean adminOnly) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        String username = body.get("username");
        String password = body.get("password");

        try {
            User user = authService.login(username, password);
            boolean isAdmin = user != null && user.getRole() == Role.ADMIN;
            if (user != null && (adminOnly == isAdmin)) {
                StringBuilder json = new StringBuilder("{");
                json.append("\"id\":").append(user.getId()).append(",");
                json.append("\"username\":\"").append(escapeJson(user.getUsername())).append("\",");
                json.append("\"fullName\":\"").append(escapeJson(user.getFullName())).append("\",");
                json.append("\"role\":\"").append(user.getRole().name()).append("\",");
                json.append("\"villageArea\":\"").append(escapeJson(user.getVillageArea() != null ? user.getVillageArea() : "")).append("\",");
                if (adminOnly) {
                    String token = UUID.randomUUID().toString();
                    adminSessions.put(token, user.getId());
                    json.append("\"token\":\"").append(token).append("\",");
                }
                if (user instanceof NGO) {
                    NGO ngo = (NGO) user;
                    json.append("\"ngoId\":").append(ngo.getNgoId()).append(",");
                    json.append("\"organizationName\":\"").append(escapeJson(ngo.getOrganizationName())).append("\",");
                    json.append("\"maxCapacity\":").append(ngo.getMaxCapacity()).append(",");
                    json.append("\"activeCases\":").append(ngo.getActiveCases()).append(",");
                    json.append("\"remainingCapacity\":").append(ngo.getRemainingCapacity()).append(",");
                    json.append("\"specializations\":").append(categoriesToJson(ngo.getSpecializations())).append(",");
                    json.append("\"serviceAreas\":").append(stringsToJson(ngo.getServiceAreas())).append(",");
                }
                json.append("\"success\":true}");
                sendJsonResponse(exchange, 200, json.toString());
            } else {
                String message = adminOnly
                    ? "Invalid admin credentials. Use an administrator account."
                    : "Invalid username/password, or admins must use Admin Login.";
                sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"" + message + "\"}");
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleAdminLogout(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : "";
        if (!token.isEmpty()) adminSessions.remove(token);
        sendJsonResponse(exchange, 200, "{\"success\":true}");
    }

    private boolean requireAdmin(HttpExchange exchange) throws IOException {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : "";
        if (token.isEmpty() || !adminSessions.containsKey(token)) {
            sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"Admin login required.\"}");
            return false;
        }
        return true;
    }

    private void handleRegisterMember(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        String username = body.get("username");
        String password = body.get("password");
        String fullName = body.get("fullName");
        String phone = body.getOrDefault("phone", "");
        String email = body.getOrDefault("email", "");
        String village = body.getOrDefault("villageArea", "");

        if (username == null || password == null || fullName == null) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Username, password, and full name are required.\"}");
            return;
        }

        try {
            boolean ok = authService.registerCommunityMember(username, password, fullName, phone, email, village);
            sendJsonResponse(exchange, 201, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleRegisterNgo(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        String username = body.get("username");
        String password = body.get("password");
        String orgName = body.get("organizationName");
        String contact = body.get("contactPerson");
        String phone = body.getOrDefault("phone", "");
        String email = body.getOrDefault("email", "");
        int capacity = 5;
        try { capacity = Integer.parseInt(body.getOrDefault("maxCapacity", "5")); } catch (Exception ignored) {}
        String specsRaw = body.getOrDefault("specializations", "");
        String areasRaw = body.getOrDefault("serviceAreas", "");

        if (username == null || password == null || orgName == null || contact == null) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Organization, contact person, username, and password are required.\"}");
            return;
        }

        try {
            NGO ngo = new NGO();
            ngo.setUsername(username);
            ngo.setOrganizationName(orgName);
            ngo.setContactPerson(contact);
            ngo.setPhone(phone);
            ngo.setEmail(email);
            ngo.setMaxCapacity(capacity);
            ngo.setVillageArea("Regional Office");

            List<Category> specs = new ArrayList<>();
            for (String s : specsRaw.split("[,;]")) {
                Category c = Category.fromString(s.trim());
                if (c != null && !specs.contains(c)) specs.add(c);
            }
            if (specs.isEmpty()) specs.add(Category.CLEAN_WATER);
            ngo.setSpecializations(specs);

            List<String> areas = new ArrayList<>();
            for (String a : areasRaw.split("[,;]")) {
                if (!a.trim().isEmpty() && !areas.contains(a.trim())) areas.add(a.trim());
            }
            if (areas.isEmpty()) areas.add("Rampur");
            ngo.setServiceAreas(areas);

            boolean ok = authService.registerNgo(ngo, password);
            sendJsonResponse(exchange, 201, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleIssues(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> params = parseQueryParams(exchange);
                if (!params.containsKey("reporterId") && !requireAdmin(exchange)) return;
                List<Issue> list;
                if (params.containsKey("reporterId")) {
                    int reporterId = Integer.parseInt(params.get("reporterId"));
                    list = issueService.getIssuesByReporter(reporterId);
                } else if (params.containsKey("status")) {
                    IssueStatus status = IssueStatus.valueOf(params.get("status"));
                    if (status == IssueStatus.PENDING_VERIFICATION) list = issueService.getPendingIssues();
                    else if (status == IssueStatus.VERIFIED) list = issueService.getVerifiedIssues();
                    else list = issueService.getAllIssues();
                } else {
                    list = issueService.getAllIssues();
                }

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(issueToJson(list.get(i)));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> body = parseJsonBody(exchange);
                Issue issue = new Issue();
                issue.setReporterId(Integer.parseInt(body.get("reporterId")));
                issue.setTitle(body.get("title"));
                issue.setCategory(Category.valueOf(body.get("category")));
                issue.setVillageArea(body.get("villageArea"));
                issue.setUrgency(Urgency.valueOf(body.getOrDefault("urgency", "MEDIUM")));
                issue.setDescription(body.get("description"));
                issue.setPhotoPath(body.getOrDefault("photoPath", ""));

                boolean ok = issueService.reportIssue(issue);
                if (ok) {
                    sendJsonResponse(exchange, 201, "{\"success\":true,\"id\":" + issue.getId() + ",\"priorityScore\":" + issue.getPriorityScore() + "}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to save issue\"}");
                }
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleVerify(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        try {
            int issueId = Integer.parseInt(body.get("issueId"));
            int adminId = Integer.parseInt(body.get("adminId"));
            String notes = body.getOrDefault("notes", "Verified by Admin");
            boolean ok = issueService.verifyIssue(issueId, adminId, notes);
            sendJsonResponse(exchange, 200, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleReject(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        try {
            int issueId = Integer.parseInt(body.get("issueId"));
            int adminId = Integer.parseInt(body.get("adminId"));
            String reason = body.getOrDefault("reason", "Declined by Admin");
            boolean ok = issueService.rejectIssue(issueId, adminId, reason);
            sendJsonResponse(exchange, 200, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleClose(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        try {
            int issueId = Integer.parseInt(body.get("issueId"));
            int adminId = Integer.parseInt(body.get("adminId"));
            String notes = body.getOrDefault("notes", "Closed by Admin");
            boolean ok = issueService.closeIssue(issueId, adminId, notes);
            sendJsonResponse(exchange, 200, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleHistory(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange);
        try {
            int issueId = Integer.parseInt(params.get("issueId"));
            List<StatusHistory> list = statusHistoryDAO.findByIssueId(issueId);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                StatusHistory sh = list.get(i);
                json.append("{")
                    .append("\"id\":").append(sh.getId()).append(",")
                    .append("\"issueId\":").append(sh.getIssueId()).append(",")
                    .append("\"newStatus\":\"").append(escapeJson(sh.getNewStatus())).append("\",")
                    .append("\"changedByName\":\"").append(escapeJson(sh.getChangedByName())).append("\",")
                    .append("\"comments\":\"").append(escapeJson(sh.getComments() != null ? sh.getComments() : "")).append("\",")
                    .append("\"changedAt\":\"").append(DateTimeUtil.format(sh.getChangedAt())).append("\"")
                    .append("}");
            }
            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleMatching(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        Map<String, String> params = parseQueryParams(exchange);
        try {
            int issueId = Integer.parseInt(params.get("issueId"));
            Issue issue = issueService.getIssueById(issueId);
            if (issue == null) {
                sendJsonResponse(exchange, 404, "{\"error\":\"Issue not found\"}");
                return;
            }

            List<MatchRecommendation> recs = matchingService.recommendNgosForIssue(issue);
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < recs.size(); i++) {
                if (i > 0) json.append(",");
                MatchRecommendation r = recs.get(i);
                NGO ngo = r.getNgo();
                json.append("{")
                    .append("\"score\":").append(r.getMatchScore()).append(",")
                    .append("\"ngoId\":").append(ngo.getNgoId()).append(",")
                    .append("\"organizationName\":\"").append(escapeJson(ngo.getOrganizationName())).append("\",")
                    .append("\"categoryMatched\":").append(r.isCategoryMatched()).append(",")
                    .append("\"areaMatched\":").append(r.isAreaMatched()).append(",")
                    .append("\"remainingCapacity\":").append(ngo.getRemainingCapacity()).append(",")
                    .append("\"maxCapacity\":").append(ngo.getMaxCapacity()).append(",")
                    .append("\"reason\":\"").append(escapeJson(r.getRecommendationReason())).append("\"")
                    .append("}");
            }
            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleAllocate(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        try {
            int issueId = Integer.parseInt(body.get("issueId"));
            int ngoId = Integer.parseInt(body.get("ngoId"));
            int adminId = Integer.parseInt(body.get("adminId"));
            String notes = body.getOrDefault("notes", "Allocated via Matching Engine");

            boolean ok = matchingService.allocateIssue(issueId, ngoId, adminId, notes);
            sendJsonResponse(exchange, 200, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleNgoTasks(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQueryParams(exchange);
        try {
            int ngoId = Integer.parseInt(params.get("ngoId"));
            List<Issue> list = issueService.getIssuesForNgo(ngoId);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                json.append(issueToJson(list.get(i)));
            }
            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleNgoStatus(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method Not Allowed");
            return;
        }
        Map<String, String> body = parseJsonBody(exchange);
        try {
            int issueId = Integer.parseInt(body.get("issueId"));
            int ngoId = Integer.parseInt(body.get("ngoId"));
            int userId = Integer.parseInt(body.get("userId"));
            IssueStatus status = IssueStatus.valueOf(body.get("status"));
            String notes = body.getOrDefault("notes", "");

            boolean ok = matchingService.updateWorkStatus(issueId, ngoId, userId, status, notes);
            sendJsonResponse(exchange, 200, "{\"success\":" + ok + "}");
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleNgos(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        try {
            List<NGO> list = ngoService.getAllNgos();
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                NGO n = list.get(i);
                json.append("{")
                    .append("\"ngoId\":").append(n.getNgoId()).append(",")
                    .append("\"organizationName\":\"").append(escapeJson(n.getOrganizationName())).append("\",")
                    .append("\"contactPerson\":\"").append(escapeJson(n.getContactPerson())).append("\",")
                    .append("\"phone\":\"").append(escapeJson(n.getPhone())).append("\",")
                    .append("\"email\":\"").append(escapeJson(n.getEmail())).append("\",")
                    .append("\"activeCases\":").append(n.getActiveCases()).append(",")
                    .append("\"maxCapacity\":").append(n.getMaxCapacity()).append(",")
                    .append("\"remainingCapacity\":").append(n.getRemainingCapacity()).append(",")
                    .append("\"specializations\":").append(categoriesToJson(n.getSpecializations())).append(",")
                    .append("\"serviceAreas\":").append(stringsToJson(n.getServiceAreas()))
                    .append("}");
            }
            json.append("]");
            sendJsonResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private void handleDashboard(HttpExchange exchange) throws IOException {
        if (!requireAdmin(exchange)) return;
        try {
            Report r = reportService.getDashboardReport();
            StringBuilder json = new StringBuilder("{");
            json.append("\"totalIssues\":").append(r.getTotalIssues()).append(",");
            json.append("\"pendingVerificationCount\":").append(r.getPendingVerificationCount()).append(",");
            json.append("\"verifiedCount\":").append(r.getVerifiedCount()).append(",");
            json.append("\"assignedCount\":").append(r.getAssignedCount()).append(",");
            json.append("\"inProgressCount\":").append(r.getInProgressCount()).append(",");
            json.append("\"resolvedCount\":").append(r.getResolvedCount()).append(",");
            json.append("\"closedCount\":").append(r.getClosedCount()).append(",");
            json.append("\"unresolvedCount\":").append(r.getUnresolvedCount()).append(",");
            json.append("\"totalNgos\":").append(r.getTotalNgos()).append(",");
            json.append("\"activeNgos\":").append(r.getActiveNgos()).append(",");

            // Categories
            json.append("\"issuesByCategory\":{");
            int c = 0;
            for (Map.Entry<String, Integer> e : r.getIssuesByCategory().entrySet()) {
                if (c++ > 0) json.append(",");
                json.append("\"").append(escapeJson(e.getKey())).append("\":").append(e.getValue());
            }
            json.append("},");

            // Areas
            json.append("\"issuesByArea\":{");
            c = 0;
            for (Map.Entry<String, Integer> e : r.getIssuesByArea().entrySet()) {
                if (c++ > 0) json.append(",");
                json.append("\"").append(escapeJson(e.getKey())).append("\":").append(e.getValue());
            }
            json.append("},");

            // NGO performance
            json.append("\"ngoPerformance\":[");
            if (r.getNgoPerformanceList() != null) {
                for (int i = 0; i < r.getNgoPerformanceList().size(); i++) {
                    if (i > 0) json.append(",");
                    Report.NgoPerformanceRecord np = r.getNgoPerformanceList().get(i);
                    json.append("{")
                        .append("\"ngoName\":\"").append(escapeJson(np.getNgoName())).append("\",")
                        .append("\"maxCapacity\":").append(np.getMaxCapacity()).append(",")
                        .append("\"activeCases\":").append(np.getActiveCases()).append(",")
                        .append("\"completedCases\":").append(np.getCompletedCases()).append(",")
                        .append("\"resolutionRate\":").append(String.format(Locale.US, "%.1f", np.getResolutionRate()))
                        .append("}");
                }
            }
            json.append("],");

            // Priority alerts
            json.append("\"priorityAlerts\":[");
            if (r.getPriorityAlerts() != null) {
                for (int i = 0; i < r.getPriorityAlerts().size(); i++) {
                    if (i > 0) json.append(",");
                    Report.PriorityAlert pa = r.getPriorityAlerts().get(i);
                    json.append("{")
                        .append("\"villageArea\":\"").append(escapeJson(pa.getVillageArea())).append("\",")
                        .append("\"category\":\"").append(escapeJson(pa.getCategory())).append("\",")
                        .append("\"complaintCount\":").append(pa.getComplaintCount()).append(",")
                        .append("\"alertLevel\":\"").append(escapeJson(pa.getAlertLevel())).append("\",")
                        .append("\"reason\":\"").append(escapeJson(pa.getReason())).append("\"")
                        .append("}");
                }
            }
            json.append("]}");

            sendJsonResponse(exchange, 200, json.toString());
        } catch (Exception e) {
            sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    // =========================================================================
    // FRONTEND WEB PORTAL
    // =========================================================================

    private void handleStaticPortal(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!"/".equals(path) && !"/index.html".equals(path)) {
            sendResponse(exchange, 404, "Not Found");
            return;
        }

        String html = getWebPortalHtml();
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getWebPortalHtml() {
        return "<!DOCTYPE html>\n" +
"<html lang=\"en\">\n" +
"<head>\n" +
"  <meta charset=\"UTF-8\">\n" +
"  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
"  <title>Rural Community Support Platform</title>\n" +
"  <style>\n" +
"    :root {\n" +
"      --primary-dark: #143629;\n" +
"      --primary: #2d6a4f;\n" +
"      --primary-light: #52b788;\n" +
"      --teal: #1b9aaa;\n" +
"      --bg: #f4f6f8;\n" +
"      --card-bg: #ffffff;\n" +
"      --text-main: #212529;\n" +
"      --text-muted: #6c757d;\n" +
"      --border: #dee2e6;\n" +
"      --critical: #d90429;\n" +
"      --high: #f77f00;\n" +
"      --resolved: #1a936f;\n" +
"    }\n" +
"    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }\n" +
"    body { background-color: var(--bg); color: var(--text-main); display: flex; flex-direction: column; min-height: 100vh; }\n" +
"    header {\n" +
"      background: linear-gradient(135deg, var(--primary-dark), var(--primary));\n" +
"      color: white; padding: 1rem 2rem; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 2px 8px rgba(0,0,0,0.15);\n" +
"    }\n" +
"    .logo-block h1 { font-size: 1.35rem; font-weight: 700; }\n" +
"    .logo-block p { font-size: 0.8rem; opacity: 0.85; margin-top: 2px; }\n" +
"    .user-pill {\n" +
"      background: rgba(255,255,255,0.15); border: 1px solid rgba(255,255,255,0.3); border-radius: 20px; padding: 6px 14px;\n" +
"      font-size: 0.85rem; display: flex; align-items: center; gap: 10px;\n" +
"    }\n" +
"    .btn-switch {\n" +
"      background: white; color: var(--primary-dark); border: none; padding: 5px 12px; border-radius: 14px;\n" +
"      font-size: 0.75rem; font-weight: bold; cursor: pointer;\n" +
"    }\n" +
"    .main-wrap { flex: 1; padding: 1.5rem 2rem; max-width: 1400px; margin: 0 auto; width: 100%; }\n" +
"    .tabs-bar {\n" +
"      display: flex; gap: 8px; border-bottom: 2px solid var(--border); margin-bottom: 1.5rem;\n" +
"    }\n" +
"    .tab-btn {\n" +
"      padding: 10px 18px; border: none; background: transparent; font-size: 0.95rem; font-weight: 600;\n" +
"      color: var(--text-muted); cursor: pointer; border-bottom: 3px solid transparent; transition: all 0.2s;\n" +
"    }\n" +
"    .tab-btn.active {\n" +
"      color: var(--primary); border-bottom-color: var(--primary); background: rgba(45,106,79,0.06); border-radius: 6px 6px 0 0;\n" +
"    }\n" +
"    .card { background: var(--card-bg); border-radius: 8px; border: 1px solid var(--border); padding: 1.25rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05); margin-bottom: 1.25rem; }\n" +
"    .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }\n" +
"    .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1.2rem; }\n" +
"    .grid-5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 1rem; margin-bottom: 1.5rem; }\n" +
"    .stat-card {\n" +
"      background: white; border-radius: 8px; border-left: 4px solid var(--primary); padding: 1rem; box-shadow: 0 1px 3px rgba(0,0,0,0.05);\n" +
"    }\n" +
"    .stat-label { font-size: 0.75rem; text-transform: uppercase; color: var(--text-muted); font-weight: 700; }\n" +
"    .stat-val { font-size: 1.8rem; font-weight: 700; color: var(--text-main); margin-top: 4px; }\n" +
"    .form-group { margin-bottom: 1rem; }\n" +
"    .form-group label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 4px; }\n" +
"    .form-control {\n" +
"      width: 100%; padding: 8px 12px; border: 1px solid var(--border); border-radius: 6px; font-size: 0.9rem;\n" +
"    }\n" +
"    .btn-pri {\n" +
"      background: var(--primary); color: white; border: none; padding: 8px 16px; border-radius: 6px; font-weight: 600; cursor: pointer;\n" +
"    }\n" +
"    .btn-sec {\n" +
"      background: white; color: var(--text-main); border: 1px solid var(--border); padding: 6px 12px; border-radius: 6px; font-weight: 600; cursor: pointer;\n" +
"    }\n" +
"    .btn-dgr {\n" +
"      background: var(--critical); color: white; border: none; padding: 6px 12px; border-radius: 6px; font-weight: 600; cursor: pointer;\n" +
"    }\n" +
"    table { width: 100%; border-collapse: collapse; font-size: 0.88rem; }\n" +
"    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--border); }\n" +
"    th { background: #f8f9fa; font-weight: 600; color: var(--primary-dark); }\n" +
"    .badge {\n" +
"      display: inline-block; padding: 3px 8px; border-radius: 12px; font-size: 0.72rem; font-weight: 700;\n" +
"    }\n" +
"    .badge-PENDING_VERIFICATION { background: #fff3cd; color: #856404; }\n" +
"    .badge-VERIFIED { background: #cce5ff; color: #004085; }\n" +
"    .badge-ASSIGNED { background: #d1ecf1; color: #0c5460; }\n" +
"    .badge-IN_PROGRESS { background: #e2e3e5; color: #383d41; }\n" +
"    .badge-RESOLVED { background: #d4edda; color: #155724; }\n" +
"    .badge-CRITICAL { background: #f8d7da; color: #721c24; }\n" +
"    .badge-HIGH { background: #ffeeba; color: #856404; }\n" +
"    .badge-score { background: #d1e7dd; color: #0f5132; font-size: 0.85rem; padding: 4px 10px; }\n" +
"    .rec-card {\n" +
"      border: 1px solid var(--border); border-radius: 8px; padding: 12px; margin-bottom: 10px; display: flex;\n" +
"      justify-content: space-between; align-items: center; background: #fafbfc;\n" +
"    }\n" +
"    .rec-card.best { border: 2px solid var(--primary-light); background: #f3faf6; }\n" +
"    .modal-overlay {\n" +
"      position: fixed; inset: 0; background: rgba(0,0,0,0.5); display: none; align-items: center; justify-content: center; z-index: 999;\n" +
"    }\n" +
"    .modal-content { background: white; border-radius: 8px; max-width: 550px; width: 90%; padding: 1.5rem; max-height: 85vh; overflow-y: auto; }\n" +
"    /* Auth Page Styles */\n" +
"    .auth-box {\n" +
"      max-width: 540px; margin: 2rem auto; background: white; border-radius: 12px;\n" +
"      box-shadow: 0 4px 20px rgba(0,0,0,0.08); border: 1px solid var(--border); overflow: hidden;\n" +
"    }\n" +
"    .auth-nav {\n" +
"      display: flex; background: #f8f9fa; border-bottom: 1px solid var(--border);\n" +
"    }\n" +
"    .auth-tab {\n" +
"      flex: 1; padding: 12px 6px; text-align: center; font-weight: 600; cursor: pointer;\n" +
"      border: none; background: transparent; color: var(--text-muted); font-size: 0.88rem;\n" +
"      border-bottom: 3px solid transparent; transition: all 0.2s;\n" +
"    }\n" +
"    .auth-tab.active {\n" +
"      color: var(--primary); background: white; border-bottom-color: var(--primary);\n" +
"    }\n" +
"    .auth-body { padding: 1.75rem; }\n" +
"    }\n" +
"    }\n" +
"  </style>\n" +
"</head>\n" +
"<body>\n" +
"  <header>\n" +
"    <div class=\"logo-block\">\n" +
"      <h1>🌱 Rural Community Support Platform</h1>\n" +
"      <p>Turning local rural problems into verified, trackable NGO action</p>\n" +
"    </div>\n" +
"    <div class=\"user-pill\">\n" +
"      <span id=\"header-user\">Welcome! Please Sign In or Register below</span>\n" +
"      <span id=\"header-controls\"></span>\n" +
"    </div>\n" +
"  </header>\n" +
"\n" +
"  <div class=\"main-wrap\">\n" +
"    <!-- Tabs -->\n" +
"    <div class=\"tabs-bar\" id=\"tabs-container\" style=\"display:none;\"></div>\n" +
"\n" +
"    <!-- Container for dynamic content -->\n" +
"    <div id=\"tab-content\"></div>\n" +
"  </div>\n" +
"\n" +
"  <!-- History Modal -->\n" +
"  <div class=\"modal-overlay\" id=\"history-modal\">\n" +
"    <div class=\"modal-content\">\n" +
"      <h3 id=\"modal-title\" style=\"margin-bottom: 12px; color: var(--primary-dark);\">Status History & Audit Timeline</h3>\n" +
"      <div id=\"modal-body\" style=\"margin-bottom: 1rem; font-size: 0.9rem;\"></div>\n" +
"      <button class=\"btn-sec\" onclick=\"closeModal()\">Close</button>\n" +
"    </div>\n" +
"  </div>\n" +
"\n" +
"  <script>\n" +
"    // State\n" +
"    let currentUser = null;\n" +
"    let activeTab = 'admin-verify';\n" +
"    let authTab = 'choice';\n" +
"    const nativeFetch = window.fetch.bind(window);\n" +
"    window.fetch = (url, options = {}) => {\n" +
"      if (currentUser && currentUser.role === 'ADMIN' && currentUser.token) {\n" +
"        options = { ...options, headers: { ...(options.headers || {}), Authorization: 'Bearer ' + currentUser.token } };\n" +
"      }\n" +
"      return nativeFetch(url, options);\n" +
"    };\n" +
"\n" +
"    function showAuthTab(t) {\n" +
"      authTab = t;\n" +
"      showAuthPage();\n" +
"    }\n" +
"\n" +
"    function showAuthPage() {\n" +
"      const c = document.getElementById('tab-content');\n" +
"      let tabContentHtml = '';\n" +
"\n" +
"      if (authTab === 'choice') {\n" +
"        tabContentHtml = `<div class=\"auth-body\" style=\"text-align:center;\"><h2 style=\"color:var(--primary-dark);margin-bottom:8px;\">Welcome</h2><p style=\"color:var(--text-muted);margin-bottom:22px;\">Choose your portal to continue.</p><button class=\"btn-pri\" style=\"width:100%;padding:13px;font-size:1rem;margin-bottom:12px;\" onclick=\"showAuthTab('login')\">👥 User / NGO Login</button><button class=\"btn-pri\" style=\"width:100%;padding:13px;font-size:1rem;background:#143629;\" onclick=\"showAuthTab('admin-login')\">🔐 Admin Login</button></div>`;\n" +
"      } else if (authTab === 'login' || authTab === 'admin-login') {\n" +
"        const adminMode = authTab === 'admin-login';\n" +
"        tabContentHtml = `\n" +
"          <h3 style=\"margin-bottom:14px;color:var(--primary-dark);text-align:center;\">${adminMode ? 'Administrator Sign In' : 'User / NGO Sign In'}</h3>\n" +
"          <div class=\"form-group\">\n" +
"            <label>Username</label>\n" +
"            <input type=\"text\" id=\"login-u\" name=\"account-${adminMode ? 'admin' : 'user'}\" class=\"form-control\" placeholder=\"${adminMode ? 'Administrator username' : 'User or NGO username'}\" value=\"\" autocomplete=\"off\" autocapitalize=\"none\" spellcheck=\"false\">\n" +
"          </div>\n" +
"          <div class=\"form-group\">\n" +
"            <label>Password</label>\n" +
"            <input type=\"password\" id=\"login-p\" name=\"secret-${adminMode ? 'admin' : 'user'}\" class=\"form-control\" placeholder=\"Your password\" value=\"\" autocomplete=\"new-password\">\n" +
"          </div>\n" +
"          <button class=\"btn-pri\" style=\"width:100%;padding:10px;font-size:1rem;margin-top:6px;\" onclick=\"doManualLogin()\">${adminMode ? 'Sign In to Admin Dashboard' : 'Sign In'}</button>\n" +
"        `;\n" +
"      } else if (authTab === 'signup-mem') {\n" +
"        tabContentHtml = `\n" +
"          <h3 style=\"margin-bottom:14px;color:var(--primary-dark);text-align:center;\">Join as Community Member</h3>\n" +
"          <div class=\"form-group\"><label>Full Name</label><input type=\"text\" id=\"reg-mem-name\" class=\"form-control\" placeholder=\"e.g. Sunita Devi\"></div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"form-group\"><label>Username</label><input type=\"text\" id=\"reg-mem-u\" class=\"form-control\" placeholder=\"sunita_d\"></div>\n" +
"            <div class=\"form-group\"><label>Password</label><input type=\"password\" id=\"reg-mem-p\" class=\"form-control\" placeholder=\"Create password\"></div>\n" +
"          </div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"form-group\"><label>Phone</label><input type=\"text\" id=\"reg-mem-phone\" class=\"form-control\" placeholder=\"+91 98765 43210\"></div>\n" +
"            <div class=\"form-group\"><label>Email</label><input type=\"email\" id=\"reg-mem-email\" class=\"form-control\" placeholder=\"sunita@village.in\"></div>\n" +
"          </div>\n" +
"          <div class=\"form-group\"><label>Village / Sector</label><input type=\"text\" id=\"reg-mem-village\" class=\"form-control\" placeholder=\"e.g. Rampur or Shivpur\" value=\"Rampur\"></div>\n" +
"          <button class=\"btn-pri\" style=\"width:100%;padding:10px;font-size:1rem;margin-top:6px;\" onclick=\"doRegisterMember()\">Create Member Account</button>\n" +
"        `;\n" +
"      } else if (authTab === 'signup-ngo') {\n" +
"        tabContentHtml = `\n" +
"          <h3 style=\"margin-bottom:14px;color:var(--primary-dark);text-align:center;\">Register NGO Organization</h3>\n" +
"          <div class=\"form-group\"><label>Organization Name</label><input type=\"text\" id=\"reg-ngo-org\" class=\"form-control\" placeholder=\"e.g. Gramin Jal Vikas Samiti\"></div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"form-group\"><label>Contact Person</label><input type=\"text\" id=\"reg-ngo-contact\" class=\"form-control\" placeholder=\"Anand Verma\"></div>\n" +
"            <div class=\"form-group\"><label>Max Concurrent Capacity</label><input type=\"number\" id=\"reg-ngo-cap\" class=\"form-control\" value=\"5\"></div>\n" +
"          </div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"form-group\"><label>Username</label><input type=\"text\" id=\"reg-ngo-u\" class=\"form-control\" placeholder=\"gramin_jal_ngo\"></div>\n" +
"            <div class=\"form-group\"><label>Password</label><input type=\"password\" id=\"reg-ngo-p\" class=\"form-control\" placeholder=\"Create password\"></div>\n" +
"          </div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"form-group\"><label>Phone</label><input type=\"text\" id=\"reg-ngo-phone\" class=\"form-control\" placeholder=\"+91 99887 76655\"></div>\n" +
"            <div class=\"form-group\"><label>Email</label><input type=\"email\" id=\"reg-ngo-email\" class=\"form-control\" placeholder=\"contact@graminjal.org\"></div>\n" +
"          </div>\n" +
"          <div class=\"form-group\"><label>Specializations (comma-separated)</label><input type=\"text\" id=\"reg-ngo-specs\" class=\"form-control\" value=\"CLEAN_WATER, SANITATION\"></div>\n" +
"          <div class=\"form-group\"><label>Service Villages (comma-separated)</label><input type=\"text\" id=\"reg-ngo-areas\" class=\"form-control\" value=\"Rampur, Belgaon\"></div>\n" +
"          <button class=\"btn-pri\" style=\"width:100%;padding:10px;font-size:1rem;margin-top:6px;\" onclick=\"doRegisterNgo()\">Register NGO Organization</button>\n" +
"        `;\n" +
"      }\n" +
"\n" +
"      c.innerHTML = `\n" +
"        <div class=\"auth-box\">\n" +
"          ${authTab === 'choice' ? '' : authTab === 'admin-login' ? `<div class=\"auth-nav\"><button class=\"auth-tab active\">Admin Sign In</button></div>` : `<div class=\"auth-nav\"><button class=\"auth-tab active\" onclick=\"showAuthTab('login')\">User / NGO Sign In</button><button class=\"auth-tab ${authTab === 'signup-mem' ? 'active' : ''}\" onclick=\"showAuthTab('signup-mem')\">Sign Up (Member)</button><button class=\"auth-tab ${authTab === 'signup-ngo' ? 'active' : ''}\" onclick=\"showAuthTab('signup-ngo')\">Sign Up (NGO)</button></div>`}\n" +
"          <div class=\"auth-body\">${tabContentHtml}</div>\n" +
"          ${authTab === 'login' || authTab === 'admin-login' ? `<div style=\"padding:0 1.75rem 1.25rem;text-align:center;\"><button class=\"btn-sec\" onclick=\"showAuthTab('choice')\">← Back to portal selection</button></div>` : ''}\n" +
"        </div>\n" +
"      `;\n" +
"    }\n" +
"\n" +
"    function doManualLogin() {\n" +
"      const u = document.getElementById('login-u').value.trim();\n" +
"      const p = document.getElementById('login-p').value;\n" +
"      if (!u || !p) { alert('Please enter username and password.'); return; }\n" +
"      doLogin(u, p);\n" +
"    }\n" +
"\n" +
"    async function doRegisterMember() {\n" +
"      const name = document.getElementById('reg-mem-name').value.trim();\n" +
"      const u = document.getElementById('reg-mem-u').value.trim();\n" +
"      const p = document.getElementById('reg-mem-p').value;\n" +
"      const phone = document.getElementById('reg-mem-phone').value.trim();\n" +
"      const email = document.getElementById('reg-mem-email').value.trim();\n" +
"      const village = document.getElementById('reg-mem-village').value.trim();\n" +
"      if (!name || !u || !p || !village) { alert('Please fill in required fields (Name, Username, Password, Village).'); return; }\n" +
"\n" +
"      const res = await fetch('/api/register/member', {\n" +
"        method: 'POST',\n" +
"        body: JSON.stringify({ fullName: name, username: u, password: p, phone, email, villageArea: village })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) {\n" +
"        alert('Registration successful! You can now sign in.');\n" +
"        showAuthTab('login');\n" +
"      } else {\n" +
"        alert('Registration failed: ' + data.error);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    async function doRegisterNgo() {\n" +
"      const org = document.getElementById('reg-ngo-org').value.trim();\n" +
"      const contact = document.getElementById('reg-ngo-contact').value.trim();\n" +
"      const cap = document.getElementById('reg-ngo-cap').value;\n" +
"      const u = document.getElementById('reg-ngo-u').value.trim();\n" +
"      const p = document.getElementById('reg-ngo-p').value;\n" +
"      const phone = document.getElementById('reg-ngo-phone').value.trim();\n" +
"      const email = document.getElementById('reg-ngo-email').value.trim();\n" +
"      const specs = document.getElementById('reg-ngo-specs').value.trim();\n" +
"      const areas = document.getElementById('reg-ngo-areas').value.trim();\n" +
"      if (!org || !contact || !u || !p) { alert('Please fill in organization name, contact person, username, and password.'); return; }\n" +
"\n" +
"      const res = await fetch('/api/register/ngo', {\n" +
"        method: 'POST',\n" +
"        body: JSON.stringify({\n" +
"          organizationName: org, contactPerson: contact, maxCapacity: cap, username: u, password: p,\n" +
"          phone, email, specializations: specs, serviceAreas: areas\n" +
"        })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) {\n" +
"        alert('NGO organization registered successfully! You can now sign in.');\n" +
"        showAuthTab('login');\n" +
"      } else {\n" +
"        alert('Registration failed: ' + data.error);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    async function doLogout() {\n" +
"      if (currentUser && currentUser.role === 'ADMIN' && currentUser.token) {\n" +
"        await fetch('/api/admin-logout', { method: 'POST' });\n" +
"      }\n" +
"      currentUser = null;\n" +
"      authTab = 'choice';\n" +
"      document.getElementById('header-user').innerText = 'Welcome! Please Sign In or Register below';\n" +
"      document.getElementById('header-controls').innerHTML = '';\n" +
"      renderTabs();\n" +
"    }\n" +
"\n" +
"    async function doLogin(u, p) {\n" +
"      const endpoint = authTab === 'admin-login' ? '/api/admin-login' : '/api/login';\n" +
"      const res = await fetch(endpoint, { method: 'POST', body: JSON.stringify({username: u, password: p}) });\n" +
"      const data = await res.json();\n" +
"      if (data.success) {\n" +
"        currentUser = data;\n" +
"        document.getElementById('header-user').innerText = `Logged in: ${data.fullName} [${data.role}]`;\n" +
"        document.getElementById('header-controls').innerHTML = `<button class=\"btn-switch\" style=\"background:#d90429;color:white;\" onclick=\"doLogout()\">Log Out</button>`;\n" +
"        renderTabs();\n" +
"      } else {\n" +
"        alert('Login failed: ' + data.error);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function renderTabs() {\n" +
"      const tc = document.getElementById('tabs-container');\n" +
"      if (!currentUser) {\n" +
"        tc.style.display = 'none';\n" +
"        showAuthPage();\n" +
"        return;\n" +
"      }\n" +
"      tc.style.display = 'flex';\n" +
"      tc.innerHTML = '';\n" +
"      if (currentUser.role === 'ADMIN') {\n" +
"        addTab(tc, 'admin-verify', '1. Verification Queue');\n" +
"        addTab(tc, 'admin-match', '2. Matching & Allocation');\n" +
"        addTab(tc, 'admin-issues', '3. All Issues Registry');\n" +
"        addTab(tc, 'admin-ngos', '4. NGO Management');\n" +
"        addTab(tc, 'admin-dash', '5. Dashboards & Analytics');\n" +
"        switchTab('admin-verify');\n" +
"      } else if (currentUser.role === 'NGO') {\n" +
"        addTab(tc, 'ngo-tasks', 'My Field Assignments');\n" +
"        addTab(tc, 'ngo-profile', 'Organization Bandwidth');\n" +
"        switchTab('ngo-tasks');\n" +
"      } else {\n" +
"        addTab(tc, 'mem-report', 'Report an Issue');\n" +
"        addTab(tc, 'mem-issues', 'My Submitted Issues');\n" +
"        switchTab('mem-report');\n" +
"      }\n" +
"    }\n" +
"\n" +
"    function addTab(container, id, label) {\n" +
"      const b = document.createElement('button');\n" +
"      b.className = 'tab-btn' + (activeTab === id ? ' active' : '');\n" +
"      b.innerText = label;\n" +
"      b.onclick = () => switchTab(id);\n" +
"      container.appendChild(b);\n" +
"    }\n" +
"\n" +
"    function switchTab(id) {\n" +
"      activeTab = id;\n" +
"      document.querySelectorAll('.tab-btn').forEach(btn => {\n" +
"        btn.classList.toggle('active', btn.innerText.includes(getTabShort(id)));\n" +
"      });\n" +
"      loadTabContent();\n" +
"    }\n" +
"\n" +
"    function getTabShort(id) {\n" +
"      if (id.includes('verify')) return 'Verification';\n" +
"      if (id.includes('match')) return 'Matching';\n" +
"      if (id.includes('issues') && !id.includes('mem')) return 'All Issues';\n" +
"      if (id.includes('ngos')) return 'NGO Management';\n" +
"      if (id.includes('dash')) return 'Dashboards';\n" +
"      if (id.includes('tasks')) return 'Field Assignments';\n" +
"      if (id.includes('profile')) return 'Organization';\n" +
"      if (id.includes('report')) return 'Report';\n" +
"      return 'My Submitted';\n" +
"    }\n" +
"\n" +
"    async function loadTabContent() {\n" +
"      const c = document.getElementById('tab-content');\n" +
"      c.innerHTML = '<p>Loading data...</p>';\n" +
"\n" +
"      if (activeTab === 'admin-verify') {\n" +
"        const res = await fetch('/api/issues?status=PENDING_VERIFICATION');\n" +
"        const list = await res.json();\n" +
"        let rows = list.map(i => `\n" +
"          <tr>\n" +
"            <td><b>#${i.id}</b></td>\n" +
"            <td><b>${escapeHtml(i.title)}</b><div style=\"font-size:0.8rem;color:var(--text-muted);\">${escapeHtml(i.description)}</div></td>\n" +
"            <td>${i.category}</td>\n" +
"            <td>${i.villageArea}</td>\n" +
"            <td><span class=\"badge badge-${i.urgency}\">${i.urgency}</span></td>\n" +
"            <td><b>${i.priorityScore}</b> / 100</td>\n" +
"            <td>${i.reporterName}</td>\n" +
"            <td>\n" +
"              <button class=\"btn-pri\" onclick=\"verifyIssue(${i.id})\">Verify</button>\n" +
"              <button class=\"btn-dgr\" onclick=\"rejectIssue(${i.id})\">Reject</button>\n" +
"              <button class=\"btn-sec\" onclick=\"viewHistory(${i.id})\">History</button>\n" +
"            </td>\n" +
"          </tr>\n" +
"        `).join('');\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\">\n" +
"            <h3 style=\"margin-bottom:12px;color:var(--primary-dark);\">Pending Verification Queue</h3>\n" +
"            <table><thead><tr><th>ID</th><th>Title & Details</th><th>Category</th><th>Village</th><th>Urgency</th><th>Priority</th><th>Reporter</th><th>Actions</th></tr></thead>\n" +
"            <tbody>${rows || '<tr><td colspan=\"8\">No pending verification complaints.</td></tr>'}</tbody></table>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'admin-match') {\n" +
"        const res = await fetch('/api/issues?status=VERIFIED');\n" +
"        const list = await res.json();\n" +
"        let issueOptions = list.map(i => `<option value=\"${i.id}\">#${i.id} - ${escapeHtml(i.title)} (${i.category}, ${i.villageArea})</option>`).join('');\n" +
"        c.innerHTML = `\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"card\">\n" +
"              <h3 style=\"margin-bottom:12px;\">1. Select Verified Issue for Matching</h3>\n" +
"              <div class=\"form-group\">\n" +
"                <select id=\"match-issue-sel\" class=\"form-control\" onchange=\"runMatching()\">\n" +
"                  <option value=\"\">-- Select Issue --</option>\n" +
"                  ${issueOptions}\n" +
"                </select>\n" +
"              </div>\n" +
"              <div id=\"selected-issue-box\" style=\"font-size:0.88rem;color:var(--text-muted);\"></div>\n" +
"            </div>\n" +
"            <div class=\"card\">\n" +
"              <h3 style=\"margin-bottom:12px;\">2. Algorithmic Recommendations (Need + Place + Capacity)</h3>\n" +
"              <div id=\"matching-results\"><p style=\"color:var(--text-muted);\">Select a verified issue on the left to compute matches.</p></div>\n" +
"            </div>\n" +
"          </div>\n" +
"        `;\n" +
"        if (list.length > 0) {\n" +
"          document.getElementById('match-issue-sel').value = list[0].id;\n" +
"          runMatching();\n" +
"        }\n" +
"      } else if (activeTab === 'admin-dash') {\n" +
"        const res = await fetch('/api/dashboard');\n" +
"        const d = await res.json();\n" +
"        let catRows = Object.entries(d.issuesByCategory).map(([k,v]) => `<tr><td>${k}</td><td><b>${v}</b></td></tr>`).join('');\n" +
"        let areaRows = Object.entries(d.issuesByArea).map(([k,v]) => `<tr><td>${k}</td><td><b>${v}</b></td></tr>`).join('');\n" +
"        let ngoRows = d.ngoPerformance.map(n => `<tr><td>${n.ngoName}</td><td>${n.activeCases}</td><td>${n.completedCases}</td><td><b>${n.resolutionRate}%</b></td></tr>`).join('');\n" +
"        let alertCards = d.priorityAlerts.map(a => `\n" +
"          <div style=\"padding:10px;border-left:4px solid var(--critical);background:#fff5f5;margin-bottom:8px;border-radius:4px;\">\n" +
"            <span class=\"badge badge-${a.alertLevel}\">${a.alertLevel}</span> <b>${a.villageArea} - ${a.category}</b> (${a.complaintCount} active)\n" +
"            <div style=\"font-size:0.85rem;color:#721c24;margin-top:2px;\">${a.reason}</div>\n" +
"          </div>\n" +
"        `).join('') || '<p style=\"color:var(--resolved);\">No emergency hotspots detected. Regions operational.</p>';\n" +
"\n" +
"        c.innerHTML = `\n" +
"          <div class=\"grid-5\">\n" +
"            <div class=\"stat-card\"><div class=\"stat-label\">Total Issues</div><div class=\"stat-val\">${d.totalIssues}</div></div>\n" +
"            <div class=\"stat-card\" style=\"border-color:var(--high);\"><div class=\"stat-label\">Unresolved</div><div class=\"stat-val\">${d.unresolvedCount}</div></div>\n" +
"            <div class=\"stat-card\" style=\"border-color:var(--teal);\"><div class=\"stat-label\">In Progress</div><div class=\"stat-val\">${d.inProgressCount}</div></div>\n" +
"            <div class=\"stat-card\" style=\"border-color:var(--resolved);\"><div class=\"stat-label\">Resolved</div><div class=\"stat-val\">${d.resolvedCount}</div></div>\n" +
"            <div class=\"stat-card\" style=\"border-color:var(--primary-dark);\"><div class=\"stat-label\">Active NGOs</div><div class=\"stat-val\">${d.activeNgos} / ${d.totalNgos}</div></div>\n" +
"          </div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"card\"><h3>Issues by Category</h3><table><thead><tr><th>Category</th><th>Count</th></tr></thead><tbody>${catRows}</tbody></table></div>\n" +
"            <div class=\"card\"><h3>Issues by Village Sector</h3><table><thead><tr><th>Village</th><th>Count</th></tr></thead><tbody>${areaRows}</tbody></table></div>\n" +
"          </div>\n" +
"          <div class=\"grid-2\">\n" +
"            <div class=\"card\"><h3>NGO Performance Scorecard</h3><table><thead><tr><th>NGO Name</th><th>Active</th><th>Completed</th><th>Resolution Rate</th></tr></thead><tbody>${ngoRows}</tbody></table></div>\n" +
"            <div class=\"card\"><h3>Priority Alerts & Hotspot Predictions</h3>${alertCards}</div>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'admin-issues') {\n" +
"        const res = await fetch('/api/issues');\n" +
"        const list = await res.json();\n" +
"        let rows = list.map(i => `\n" +
"          <tr>\n" +
"            <td><b>#${i.id}</b></td>\n" +
"            <td><b>${escapeHtml(i.title)}</b></td>\n" +
"            <td>${i.category}</td>\n" +
"            <td>${i.villageArea}</td>\n" +
"            <td><span class=\"badge badge-${i.status}\">${i.status}</span></td>\n" +
"            <td><b>${i.priorityScore}</b></td>\n" +
"            <td>${i.assignedNgoName || 'None'}</td>\n" +
"            <td>\n" +
"              <button class=\"btn-sec\" onclick=\"viewHistory(${i.id})\">Timeline</button>\n" +
"              ${i.status === 'RESOLVED' ? `<button class=\"btn-pri\" onclick=\"closeIssue(${i.id})\">Close Ticket</button>` : ''}\n" +
"            </td>\n" +
"          </tr>\n" +
"        `).join('');\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\">\n" +
"            <h3 style=\"margin-bottom:12px;\">Master Issues Registry</h3>\n" +
"            <table><thead><tr><th>ID</th><th>Title</th><th>Category</th><th>Village</th><th>Status</th><th>Score</th><th>Assigned NGO</th><th>Action</th></tr></thead>\n" +
"            <tbody>${rows}</tbody></table>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'admin-ngos') {\n" +
"        const res = await fetch('/api/ngos');\n" +
"        const list = await res.json();\n" +
"        let rows = list.map(n => `\n" +
"          <tr>\n" +
"            <td><b>${escapeHtml(n.organizationName)}</b></td>\n" +
"            <td>${n.contactPerson}</td>\n" +
"            <td>${n.phone}</td>\n" +
"            <td><b>${n.activeCases} / ${n.maxCapacity}</b> (${n.remainingCapacity} free)</td>\n" +
"            <td>${n.specializations.join(', ')}</td>\n" +
"            <td>${n.serviceAreas.join(', ')}</td>\n" +
"          </tr>\n" +
"        `).join('');\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\">\n" +
"            <h3 style=\"margin-bottom:12px;\">Registered NGO Directory</h3>\n" +
"            <table><thead><tr><th>Organization</th><th>Contact</th><th>Phone</th><th>Caseload (Active/Max)</th><th>Specializations</th><th>Service Areas</th></tr></thead>\n" +
"            <tbody>${rows}</tbody></table>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'mem-report') {\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\" style=\"max-width:650px;margin:0 auto;\">\n" +
"            <h3 style=\"margin-bottom:16px;color:var(--primary-dark);\">Report a Rural Community Issue</h3>\n" +
"            <div class=\"form-group\">\n" +
"              <label>Issue Title</label>\n" +
"              <input type=\"text\" id=\"r-title\" class=\"form-control\" placeholder=\"e.g. Broken handpump on main street\">\n" +
"            </div>\n" +
"            <div class=\"grid-2\">\n" +
"              <div class=\"form-group\">\n" +
"                <label>Category</label>\n" +
"                <select id=\"r-cat\" class=\"form-control\">\n" +
"                  <option value=\"CLEAN_WATER\">Clean Water</option>\n" +
"                  <option value=\"SANITATION\">Sanitation</option>\n" +
"                  <option value=\"HEALTHCARE\">Healthcare</option>\n" +
"                  <option value=\"EDUCATION\">Education</option>\n" +
"                  <option value=\"WASTE_MANAGEMENT\">Waste Management</option>\n" +
"                </select>\n" +
"              </div>\n" +
"              <div class=\"form-group\">\n" +
"                <label>Village / Sector</label>\n" +
"                <input type=\"text\" id=\"r-village\" class=\"form-control\" value=\"${currentUser.villageArea || ''}\">\n" +
"              </div>\n" +
"            </div>\n" +
"            <div class=\"form-group\">\n" +
"              <label>Urgency Level</label>\n" +
"              <select id=\"r-urg\" class=\"form-control\">\n" +
"                <option value=\"LOW\">Low</option>\n" +
"                <option value=\"MEDIUM\" selected>Medium</option>\n" +
"                <option value=\"HIGH\">High</option>\n" +
"                <option value=\"CRITICAL\">Critical Emergency</option>\n" +
"              </select>\n" +
"            </div>\n" +
"            <div class=\"form-group\">\n" +
"              <label>Detailed Description</label>\n" +
"              <textarea id=\"r-desc\" class=\"form-control\" rows=\"4\" placeholder=\"Describe the problem, affected families, and location\"></textarea>\n" +
"            </div>\n" +
"            <button class=\"btn-pri\" style=\"width:100%;padding:10px;font-size:1rem;\" onclick=\"submitReport()\">Submit Complaint</button>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'mem-issues') {\n" +
"        const res = await fetch('/api/issues?reporterId=' + currentUser.id);\n" +
"        const list = await res.json();\n" +
"        let rows = list.map(i => `\n" +
"          <tr>\n" +
"            <td><b>#${i.id}</b></td>\n" +
"            <td><b>${escapeHtml(i.title)}</b></td>\n" +
"            <td>${i.category}</td>\n" +
"            <td>${i.villageArea}</td>\n" +
"            <td><span class=\"badge badge-${i.status}\">${i.status}</span></td>\n" +
"            <td><b>${i.priorityScore}</b></td>\n" +
"            <td><button class=\"btn-sec\" onclick=\"viewHistory(${i.id})\">View Timeline</button></td>\n" +
"          </tr>\n" +
"        `).join('');\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\">\n" +
"            <h3 style=\"margin-bottom:12px;\">My Submitted Issues & Progress</h3>\n" +
"            <table><thead><tr><th>ID</th><th>Title</th><th>Category</th><th>Village</th><th>Status</th><th>Score</th><th>Action</th></tr></thead>\n" +
"            <tbody>${rows || '<tr><td colspan=\"7\">No reported issues yet.</td></tr>'}</tbody></table>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'ngo-tasks') {\n" +
"        const res = await fetch('/api/ngo/tasks?ngoId=' + (currentUser.ngoId || 1));\n" +
"        const list = await res.json();\n" +
"        let rows = list.map(i => `\n" +
"          <tr>\n" +
"            <td><b>#${i.id}</b></td>\n" +
"            <td><b>${escapeHtml(i.title)}</b><div style=\"font-size:0.8rem;color:var(--text-muted);\">${escapeHtml(i.description)}</div></td>\n" +
"            <td>${i.category}</td>\n" +
"            <td>${i.villageArea}</td>\n" +
"            <td><span class=\"badge badge-${i.status}\">${i.status}</span></td>\n" +
"            <td>${i.reporterName} (${i.reporterPhone})</td>\n" +
"            <td>\n" +
"              ${i.status === 'ASSIGNED' ? `<button class=\"btn-sec\" onclick=\"updateNgoStatus(${i.id}, 'IN_PROGRESS')\">Start Work</button>` : ''}\n" +
"              ${i.status === 'IN_PROGRESS' ? `<button class=\"btn-pri\" onclick=\"updateNgoStatus(${i.id}, 'RESOLVED')\">Mark Resolved</button>` : ''}\n" +
"              <button class=\"btn-sec\" onclick=\"viewHistory(${i.id})\">History</button>\n" +
"            </td>\n" +
"          </tr>\n" +
"        `).join('');\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\">\n" +
"            <h3 style=\"margin-bottom:12px;\">Assigned Field Operations</h3>\n" +
"            <table><thead><tr><th>ID</th><th>Title & Details</th><th>Category</th><th>Village</th><th>Status</th><th>Reporter</th><th>Action</th></tr></thead>\n" +
"            <tbody>${rows || '<tr><td colspan=\"7\">No tasks currently assigned.</td></tr>'}</tbody></table>\n" +
"          </div>\n" +
"        `;\n" +
"      } else if (activeTab === 'ngo-profile') {\n" +
"        c.innerHTML = `\n" +
"          <div class=\"card\" style=\"max-width:600px;margin:0 auto;\">\n" +
"            <h3 style=\"margin-bottom:14px;color:var(--primary-dark);\">Organization Profile & Capacity</h3>\n" +
"            <p><b>Organization:</b> ${currentUser.organizationName || 'Jal Seva Mission'}</p>\n" +
"            <p style=\"margin-top:8px;\"><b>Caseload:</b> ${currentUser.activeCases || 0} active / ${currentUser.maxCapacity || 6} maximum</p>\n" +
"            <p style=\"margin-top:8px;\"><b>Free Slots:</b> <span class=\"badge badge-score\">${currentUser.remainingCapacity || 6} slots available</span></p>\n" +
"            <p style=\"margin-top:8px;\"><b>Specializations:</b> ${(currentUser.specializations || ['CLEAN_WATER', 'WASTE_MANAGEMENT']).join(', ')}</p>\n" +
"            <p style=\"margin-top:8px;\"><b>Service Areas:</b> ${(currentUser.serviceAreas || ['Rampur', 'Shivpur']).join(', ')}</p>\n" +
"          </div>\n" +
"        `;\n" +
"      }\n" +
"    }\n" +
"\n" +
"    async function submitReport() {\n" +
"      const title = document.getElementById('r-title').value.trim();\n" +
"      const cat = document.getElementById('r-cat').value;\n" +
"      const village = document.getElementById('r-village').value.trim();\n" +
"      const urg = document.getElementById('r-urg').value;\n" +
"      const desc = document.getElementById('r-desc').value.trim();\n" +
"      if (!title || !village || !desc) { alert('Please enter title, village, and description.'); return; }\n" +
"\n" +
"      const res = await fetch('/api/issues', {\n" +
"        method: 'POST',\n" +
"        body: JSON.stringify({\n" +
"          reporterId: currentUser.id, title, category: cat, villageArea: village, urgency: urg, description: desc\n" +
"        })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) {\n" +
"        alert(`Issue #${data.id} submitted successfully! Priority Score: ${data.priorityScore}`);\n" +
"        switchTab('mem-issues');\n" +
"      } else {\n" +
"        alert('Error: ' + data.error);\n" +
"      }\n" +
"    }\n" +
"\n" +
"    async function verifyIssue(id) {\n" +
"      const notes = prompt('Enter verification notes (e.g., cross-verified with Panchayat):', 'Verified by Coordinator');\n" +
"      if (!notes) return;\n" +
"      const res = await fetch('/api/verify', {\n" +
"        method: 'POST', body: JSON.stringify({ issueId: id, adminId: currentUser.id, notes })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) { alert('Issue #' + id + ' verified!'); loadTabContent(); }\n" +
"    }\n" +
"\n" +
"    async function rejectIssue(id) {\n" +
"      const reason = prompt('Enter rejection reason:');\n" +
"      if (!reason) return;\n" +
"      const res = await fetch('/api/reject', {\n" +
"        method: 'POST', body: JSON.stringify({ issueId: id, adminId: currentUser.id, reason })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) { alert('Issue #' + id + ' rejected.'); loadTabContent(); }\n" +
"    }\n" +
"\n" +
"    async function closeIssue(id) {\n" +
"      const notes = prompt('Enter closing remarks:', 'Confirmed work completion');\n" +
"      if (!notes) return;\n" +
"      const res = await fetch('/api/close', {\n" +
"        method: 'POST', body: JSON.stringify({ issueId: id, adminId: currentUser.id, notes })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) { alert('Issue #' + id + ' closed.'); loadTabContent(); }\n" +
"    }\n" +
"\n" +
"    async function runMatching() {\n" +
"      const id = document.getElementById('match-issue-sel').value;\n" +
"      const box = document.getElementById('matching-results');\n" +
"      if (!id) { box.innerHTML = '<p>Select an issue to run matches.</p>'; return; }\n" +
"\n" +
"      box.innerHTML = '<p>Computing matches with Need, Place, and Capacity algorithm...</p>';\n" +
"      const res = await fetch('/api/matching?issueId=' + id);\n" +
"      const recs = await res.json();\n" +
"\n" +
"      box.innerHTML = recs.map((r, idx) => `\n" +
"        <div class=\"rec-card ${idx === 0 ? 'best' : ''}\">\n" +
"          <div>\n" +
"            <div style=\"font-weight:700;font-size:1rem;\">${escapeHtml(r.organizationName)}</div>\n" +
"            <div style=\"font-size:0.82rem;color:var(--text-muted);margin-top:2px;\">${escapeHtml(r.reason)}</div>\n" +
"            <div style=\"font-size:0.8rem;margin-top:4px;\">\n" +
"              Category: <b>${r.categoryMatched ? 'YES' : 'NO'}</b> | Area: <b>${r.areaMatched ? 'YES' : 'NO'}</b> | Free: <b>${r.remainingCapacity} / ${r.maxCapacity}</b>\n" +
"            </div>\n" +
"          </div>\n" +
"          <div style=\"text-align:right;\">\n" +
"            <div class=\"badge badge-score\" style=\"font-size:1.1rem;\">${r.score}% Match</div>\n" +
"            <div style=\"margin-top:6px;\"><button class=\"btn-pri\" onclick=\"allocateIssue(${id}, ${r.ngoId}, '${escapeHtml(r.organizationName)}')\">Allocate</button></div>\n" +
"          </div>\n" +
"        </div>\n" +
"      `).join('') || '<p>No NGOs available.</p>';\n" +
"    }\n" +
"\n" +
"    async function allocateIssue(issueId, ngoId, name) {\n" +
"      const notes = prompt(`Confirm allocation to ${name}. Notes:`, 'Allocated via matching engine');\n" +
"      if (notes === null) return;\n" +
"      const res = await fetch('/api/allocate', {\n" +
"        method: 'POST', body: JSON.stringify({ issueId, ngoId, adminId: currentUser.id, notes })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) {\n" +
"        alert(`Successfully allocated Issue #${issueId} to ${name}!`);\n" +
"        loadTabContent();\n" +
"      }\n" +
"    }\n" +
"\n" +
"    async function updateNgoStatus(issueId, status) {\n" +
"      const promptText = status === 'RESOLVED' ? 'Resolution remarks (action taken):' : 'Starting work progress notes:';\n" +
"      const notes = prompt(promptText, status === 'RESOLVED' ? 'Repairs completed and inspected' : 'Team dispatched');\n" +
"      if (notes === null) return;\n" +
"      const res = await fetch('/api/ngo/status', {\n" +
"        method: 'POST', body: JSON.stringify({ issueId, ngoId: currentUser.ngoId || 1, userId: currentUser.id, status, notes })\n" +
"      });\n" +
"      const data = await res.json();\n" +
"      if (data.success) {\n" +
"        alert('Status updated to ' + status);\n" +
"        loadTabContent();\n" +
"      }\n" +
"    }\n" +
"\n" +
"    async function viewHistory(id) {\n" +
"      const res = await fetch('/api/history?issueId=' + id);\n" +
"      const list = await res.json();\n" +
"      let html = list.map(h => `\n" +
"        <div style=\"border-bottom:1px solid #eee;padding:8px 0;\">\n" +
"          <div style=\"display:flex;justify-content:space-between;\">\n" +
"            <span class=\"badge badge-${h.newStatus}\">${h.newStatus}</span>\n" +
"            <small style=\"color:var(--text-muted);\">${h.changedAt}</small>\n" +
"          </div>\n" +
"          <div style=\"margin-top:4px;\"><b>By:</b> ${escapeHtml(h.changedByName)}</div>\n" +
"          <div style=\"color:var(--text-muted);margin-top:2px;\"><b>Notes:</b> ${escapeHtml(h.comments || 'None')}</div>\n" +
"        </div>\n" +
"      `).join('') || '<p>No history available.</p>';\n" +
"      document.getElementById('modal-title').innerText = 'Audit Timeline for Issue #' + id;\n" +
"      document.getElementById('modal-body').innerHTML = html;\n" +
"      document.getElementById('history-modal').style.display = 'flex';\n" +
"    }\n" +
"\n" +
"    function closeModal() {\n" +
"      document.getElementById('history-modal').style.display = 'none';\n" +
"    }\n" +
"\n" +
"    function escapeHtml(s) {\n" +
"      if (!s) return '';\n" +
"      return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;');\n" +
"    }\n" +
"\n" +
"    // Initialize\n" +
"    renderTabs();\n" +
"  </script>\n" +
"</body>\n" +
"</html>\n";
    }

    // =========================================================================
    // JSON & HTTP UTILITIES
    // =========================================================================

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(HttpExchange exchange) {
        Map<String, String> map = new HashMap<>();
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private Map<String, String> parseJsonBody(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+|true|false))");
        java.util.regex.Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            String key = matcher.group(1);
            String val = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            map.put(key, val);
        }
        return map;
    }

    private String issueToJson(Issue i) {
        return "{" +
                "\"id\":" + i.getId() + "," +
                "\"reporterId\":" + i.getReporterId() + "," +
                "\"reporterName\":\"" + escapeJson(i.getReporterName()) + "\"," +
                "\"reporterPhone\":\"" + escapeJson(i.getReporterPhone()) + "\"," +
                "\"title\":\"" + escapeJson(i.getTitle()) + "\"," +
                "\"category\":\"" + i.getCategory().name() + "\"," +
                "\"villageArea\":\"" + escapeJson(i.getVillageArea()) + "\"," +
                "\"urgency\":\"" + i.getUrgency().name() + "\"," +
                "\"description\":\"" + escapeJson(i.getDescription()) + "\"," +
                "\"status\":\"" + i.getStatus().name() + "\"," +
                "\"priorityScore\":" + i.getPriorityScore() + "," +
                "\"assignedNgoId\":" + i.getAssignedNgoId() + "," +
                "\"assignedNgoName\":\"" + escapeJson(i.getAssignedNgoName() != null ? i.getAssignedNgoName() : "") + "\"," +
                "\"createdAt\":\"" + DateTimeUtil.format(i.getCreatedAt()) + "\"" +
                "}";
    }

    private String categoriesToJson(List<Category> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(list.get(i).name()).append("\"");
        }
        return sb.append("]").toString();
    }

    private String stringsToJson(List<String> list) {
        if (list == null) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
