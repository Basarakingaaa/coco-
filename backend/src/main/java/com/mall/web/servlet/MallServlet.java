package com.mall.web.servlet;

import com.mall.web.util.Db;
import com.mall.web.util.Web;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@MultipartConfig
@WebServlet("/api/*")
public class MallServlet extends HttpServlet {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        handle(request, response);
    }

    private void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String path = Optional.ofNullable(request.getPathInfo()).orElse("/");
        try {
            if (path.equals("/session")) session(request, response);
            else if (path.equals("/users/login") && method.equals("POST")) login(request, response);
            else if (path.equals("/users/logout") && method.equals("POST")) logout(request, response);
            else if (path.equals("/users/send-code") && method.equals("POST")) Web.ok(response, Map.of("sent", true, "code", "123456"));
            else if (path.equals("/users/register") && method.equals("POST")) register(request, response);
            else if (path.equals("/categories") && method.equals("GET")) categories(request, response);
            else if (path.equals("/ads") && method.equals("GET")) ads(request, response);
            else if (path.equals("/products") && method.equals("GET")) products(request, response);
            else if (path.matches("/products/\\d+") && method.equals("GET")) productDetail(pathId(path), response);
            else if (path.equals("/cart") && method.equals("GET")) cart(request, response);
            else if (path.equals("/cart") && method.equals("POST")) addCart(request, response);
            else if (path.matches("/cart/\\d+") && method.equals("PUT")) updateCart(pathId(path), request, response);
            else if (path.equals("/cart") && method.equals("DELETE")) deleteCart(request, response);
            else if (path.equals("/orders/checkout") && method.equals("POST")) checkout(request, response);
            else if (path.equals("/orders") && method.equals("GET")) orders(request, response);
            else if (path.matches("/orders/\\d+") && method.equals("GET")) orderDetail(pathId(path), request, response);
            else if (path.matches("/orders/\\d+/pay") && method.equals("POST")) orderStatus(pathId(path), 1, request, response);
            else if (path.matches("/orders/\\d+/cancel") && method.equals("POST")) orderStatus(pathId(path), 4, request, response);
            else if (path.equals("/admin/files/upload") && method.equals("POST")) upload(request, response);
            else if (path.equals("/admin/products") && method.equals("GET")) adminProducts(response);
            else if (path.equals("/admin/products") && method.equals("POST")) adminSaveProduct(0, request, response);
            else if (path.matches("/admin/products/\\d+") && method.equals("GET")) productDetail(pathId(path), response);
            else if (path.matches("/admin/products/\\d+") && method.equals("PUT")) adminSaveProduct(pathId(path), request, response);
            else if (path.matches("/admin/products/\\d+") && method.equals("DELETE")) softDelete("t_product", pathId(path), response);
            else if (path.matches("/admin/products/\\d+/media") && method.equals("GET")) adminMedia(pathId(path), response);
            else if (path.matches("/admin/products/\\d+/media") && method.equals("POST")) adminAddMedia(pathId(path), request, response);
            else if (path.matches("/admin/products/\\d+/media/\\d+") && method.equals("DELETE")) adminDeleteMedia(pathIds(path)[1], response);
            else if (path.equals("/admin/categories") && method.equals("GET")) Web.ok(response, Db.list("SELECT * FROM t_product_category ORDER BY sort_order,id"));
            else if (path.equals("/admin/categories") && method.equals("POST")) adminSaveCategory(0, request, response);
            else if (path.matches("/admin/categories/\\d+") && method.equals("PUT")) adminSaveCategory(pathId(path), request, response);
            else if (path.matches("/admin/categories/\\d+") && method.equals("DELETE")) softDelete("t_product_category", pathId(path), response);
            else if (path.equals("/admin/ads") && method.equals("GET")) Web.ok(response, Db.list("SELECT a.*,c.name category_name FROM t_advertisement a LEFT JOIN t_ad_category c ON a.category_id=c.id ORDER BY a.sort_order,a.id"));
            else if (path.equals("/admin/ads") && method.equals("POST")) adminSaveAd(0, request, response);
            else if (path.matches("/admin/ads/\\d+") && method.equals("PUT")) adminSaveAd(pathId(path), request, response);
            else if (path.matches("/admin/ads/\\d+") && method.equals("DELETE")) softDelete("t_advertisement", pathId(path), response);
            else if (path.equals("/admin/ads/categories") && method.equals("GET")) Web.ok(response, Db.list("SELECT * FROM t_ad_category ORDER BY id"));
            else if (path.equals("/admin/ads/categories") && method.equals("POST")) createAdCategory(request, response);
            else if (path.equals("/admin/orders") && method.equals("GET")) Web.ok(response, Db.list("SELECT o.*,u.username FROM t_order o LEFT JOIN t_user u ON o.user_id=u.id ORDER BY o.id DESC"));
            else if (path.matches("/admin/orders/\\d+/status") && method.equals("PUT")) adminOrderStatus(pathId(path), request, response);
            else if (path.equals("/admin/users") && method.equals("GET")) Web.ok(response, Db.list("SELECT id,username,phone,email,role,status,created_at,updated_at FROM t_user ORDER BY id DESC"));
            else if (path.matches("/admin/users/\\d+/status") && method.equals("PUT")) adminUserStatus(pathId(path), request, response);
            else Web.fail(response, 404, "接口不存在");
        } catch (AuthException exception) {
            Web.fail(response, 401, exception.getMessage());
        } catch (Exception exception) {
            exception.printStackTrace();
            Web.fail(response, 500, exception.getMessage() == null ? "服务器异常" : exception.getMessage());
        }
    }

    private void login(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> body = Web.body(request);
        Map<String, Object> user = Db.one("SELECT * FROM t_user WHERE username=?", Web.string(body.get("username")));
        if (user == null || Web.intValue(user.get("status"), 0) != 1 || !passwordEncoder.matches(Web.string(body.get("password")), Web.string(user.get("password")))) {
            Web.fail(response, 401, "用户名或密码错误");
            return;
        }
        request.getSession(true).setAttribute("userId", Web.longValue(user.get("id")));
        request.getSession(true).setAttribute("username", user.get("username"));
        request.getSession(true).setAttribute("role", user.get("role"));
        Web.ok(response, Map.of("username", user.get("username"), "role", user.get("role")));
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        Web.ok(response, true);
    }

    private void session(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            Web.ok(response, null);
            return;
        }
        Web.ok(response, Map.of("id", session.getAttribute("userId"), "username", session.getAttribute("username"), "role", session.getAttribute("role")));
    }

    private void register(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> body = Web.body(request);
        String username = Web.string(body.get("username"));
        if (Db.one("SELECT id FROM t_user WHERE username=?", username) != null) {
            Web.fail(response, 400, "用户名已存在");
            return;
        }
        long id = Db.insert("INSERT INTO t_user(username,password,phone,email,role,status,created_at,updated_at) VALUES(?,?,?,?, 'USER',1,NOW(),NOW())",
                username, passwordEncoder.encode(Web.string(body.get("password"))), Web.string(body.get("phone")), Web.string(body.get("email")));
        Web.ok(response, Map.of("id", id));
    }

    private void categories(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String parentId = request.getParameter("parentId");
        if (parentId == null || parentId.isBlank()) {
            Web.ok(response, Db.list("SELECT * FROM t_product_category WHERE status=1 ORDER BY sort_order,id"));
        } else {
            Web.ok(response, Db.list("SELECT * FROM t_product_category WHERE status=1 AND parent_id=? ORDER BY sort_order,id", parentId));
        }
    }

    private void ads(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String categoryId = request.getParameter("categoryId");
        if (categoryId == null || categoryId.isBlank()) {
            Web.ok(response, Db.list("SELECT * FROM t_advertisement WHERE status=1 ORDER BY sort_order,id"));
        } else {
            Web.ok(response, Db.list("SELECT * FROM t_advertisement WHERE status=1 AND category_id=? ORDER BY sort_order,id", categoryId));
        }
    }

    private void products(HttpServletRequest request, HttpServletResponse response) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT p.*,c.name category_name,(SELECT url FROM t_product_media m WHERE m.product_id=p.id ORDER BY sort_order,id LIMIT 1) cover FROM t_product p LEFT JOIN t_product_category c ON p.category_id=c.id WHERE p.status=1");
        List<Object> args = new ArrayList<>();
        if (request.getParameter("categoryId") != null && !request.getParameter("categoryId").isBlank()) {
            sql.append(" AND p.category_id=?");
            args.add(request.getParameter("categoryId"));
        }
        if (request.getParameter("keyword") != null && !request.getParameter("keyword").isBlank()) {
            sql.append(" AND p.name LIKE ?");
            args.add("%" + request.getParameter("keyword") + "%");
        }
        sql.append(" ORDER BY p.id DESC");
        Web.ok(response, Db.list(sql.toString(), args.toArray()));
    }

    private void productDetail(long id, HttpServletResponse response) throws Exception {
        Map<String, Object> product = Db.one("SELECT p.*,c.name category_name FROM t_product p LEFT JOIN t_product_category c ON p.category_id=c.id WHERE p.id=?", id);
        if (product == null) {
            Web.fail(response, 404, "商品不存在");
            return;
        }
        product.put("media", Db.list("SELECT * FROM t_product_media WHERE product_id=? ORDER BY sort_order,id", id));
        product.put("skus", Db.list("SELECT * FROM t_product_sku WHERE product_id=? ORDER BY id", id));
        product.put("infos1", Db.list("SELECT * FROM t_product_info1 WHERE product_id=? ORDER BY sort_order,id", id));
        product.put("infos2", Db.list("SELECT * FROM t_product_info2 WHERE product_id=? ORDER BY sort_order,id", id));
        Web.ok(response, product);
    }

    private void cart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long userId = requireUser(request);
        Web.ok(response, Db.list("SELECT ci.*,p.name product_name,p.price,p.stock,(SELECT url FROM t_product_media m WHERE m.product_id=p.id ORDER BY sort_order,id LIMIT 1) cover FROM t_cart_item ci JOIN t_product p ON ci.product_id=p.id WHERE ci.user_id=? ORDER BY ci.id DESC", userId));
    }

    private void addCart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long userId = requireUser(request);
        Map<String, Object> body = Web.body(request);
        long productId = Web.longValue(body.get("productId"));
        int quantity = Web.intValue(body.get("quantity"), 1);
        Map<String, Object> existing = Db.one("SELECT id,quantity FROM t_cart_item WHERE user_id=? AND product_id=?", userId, productId);
        if (existing == null) {
            Db.insert("INSERT INTO t_cart_item(user_id,product_id,sku_id,quantity,selected,created_at,updated_at) VALUES(?,?,NULL,?,1,NOW(),NOW())", userId, productId, quantity);
        } else {
            Db.update("UPDATE t_cart_item SET quantity=quantity+?,updated_at=NOW() WHERE id=?", quantity, existing.get("id"));
        }
        cart(request, response);
    }

    private void updateCart(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireUser(request);
        Map<String, Object> body = Web.body(request);
        Db.update("UPDATE t_cart_item SET quantity=?,updated_at=NOW() WHERE id=?", Web.intValue(body.get("quantity"), 1), id);
        Web.ok(response, true);
    }

    private void deleteCart(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireUser(request);
        Map<String, Object> body = Web.body(request);
        Object ids = body.get("ids");
        if (ids instanceof List<?> list) {
            for (Object id : list) Db.update("DELETE FROM t_cart_item WHERE id=?", Web.longValue(id));
        }
        Web.ok(response, true);
    }

    private void checkout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        long userId = requireUser(request);
        Map<String, Object> body = Web.body(request);
        List<Map<String, Object>> items = Db.list("SELECT ci.*,p.name product_name,p.price,(SELECT url FROM t_product_media m WHERE m.product_id=p.id ORDER BY sort_order,id LIMIT 1) cover FROM t_cart_item ci JOIN t_product p ON ci.product_id=p.id WHERE ci.user_id=?", userId);
        if (items.isEmpty()) {
            Web.fail(response, 400, "购物车为空");
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            total = total.add(new BigDecimal(String.valueOf(item.get("price"))).multiply(BigDecimal.valueOf(Web.longValue(item.get("quantity")))));
        }
        try (Connection connection = Db.conn()) {
            connection.setAutoCommit(false);
            try {
                String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + userId;
                long orderId;
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO t_order(order_no,user_id,total_amount,status,receiver_name,receiver_phone,receiver_address,created_at,updated_at) VALUES(?,?,?,0,?,?,?,NOW(),NOW())", Statement.RETURN_GENERATED_KEYS)) {
                    Db.bind(statement, orderNo, userId, total, Web.string(body.get("receiverName")), Web.string(body.get("receiverPhone")), Web.string(body.get("receiverAddress")));
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        orderId = keys.getLong(1);
                    }
                }
                for (Map<String, Object> item : items) {
                    try (PreparedStatement statement = connection.prepareStatement("INSERT INTO t_order_item(order_id,product_id,sku_id,product_name,product_image,price,quantity) VALUES(?,?,?,?,?,?,?)")) {
                        Db.bind(statement, orderId, item.get("product_id"), item.get("sku_id"), item.get("product_name"), item.get("cover"), item.get("price"), item.get("quantity"));
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM t_cart_item WHERE user_id=?")) {
                    Db.bind(statement, userId);
                    statement.executeUpdate();
                }
                connection.commit();
                Web.ok(response, Map.of("id", orderId, "orderNo", orderNo));
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private void orders(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Web.ok(response, Db.list("SELECT * FROM t_order WHERE user_id=? ORDER BY id DESC", requireUser(request)));
    }

    private void orderDetail(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        long userId = requireUser(request);
        Map<String, Object> order = Db.one("SELECT * FROM t_order WHERE id=? AND user_id=?", id, userId);
        if (order == null) {
            Web.fail(response, 404, "订单不存在");
            return;
        }
        order.put("items", Db.list("SELECT * FROM t_order_item WHERE order_id=?", id));
        Web.ok(response, order);
    }

    private void orderStatus(long id, int status, HttpServletRequest request, HttpServletResponse response) throws Exception {
        long userId = requireUser(request);
        Db.update("UPDATE t_order SET status=?,paid_at=IF(?=1,NOW(),paid_at),updated_at=NOW() WHERE id=? AND user_id=?", status, status, id, userId);
        Web.ok(response, true);
    }

    private void adminProducts(HttpServletResponse response) throws Exception {
        Web.ok(response, Db.list("SELECT p.*,c.name category_name,(SELECT url FROM t_product_media m WHERE m.product_id=p.id ORDER BY sort_order,id LIMIT 1) cover FROM t_product p LEFT JOIN t_product_category c ON p.category_id=c.id ORDER BY p.id DESC"));
    }

    private void adminSaveProduct(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        if (id == 0) {
            id = Db.insert("INSERT INTO t_product(category_id,name,subtitle,price,stock,sales,status,created_at,updated_at) VALUES(?,?,?,?,?,0,?,NOW(),NOW())",
                    Web.longValue(b.get("categoryId")), Web.string(b.get("name")), Web.string(b.get("subtitle")), b.get("price"), Web.intValue(b.get("stock"), 0), Web.intValue(b.get("status"), 1));
        } else {
            Db.update("UPDATE t_product SET category_id=?,name=?,subtitle=?,price=?,stock=?,status=?,updated_at=NOW() WHERE id=?",
                    Web.longValue(b.get("categoryId")), Web.string(b.get("name")), Web.string(b.get("subtitle")), b.get("price"), Web.intValue(b.get("stock"), 0), Web.intValue(b.get("status"), 1), id);
        }
        productDetail(id, response);
    }

    private void adminMedia(long productId, HttpServletResponse response) throws Exception {
        Web.ok(response, Db.list("SELECT * FROM t_product_media WHERE product_id=? ORDER BY sort_order,id", productId));
    }

    private void adminAddMedia(long productId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        long id = Db.insert("INSERT INTO t_product_media(product_id,media_type,url,sort_order,created_at) VALUES(?,?,?,?,NOW())", productId, Web.string(b.get("mediaType")).isBlank() ? "IMAGE" : Web.string(b.get("mediaType")), Web.string(b.get("url")), Web.intValue(b.get("sortOrder"), 0));
        Web.ok(response, Db.one("SELECT * FROM t_product_media WHERE id=?", id));
    }

    private void adminDeleteMedia(long mediaId, HttpServletResponse response) throws Exception {
        Db.update("DELETE FROM t_product_media WHERE id=?", mediaId);
        Web.ok(response, true);
    }

    private void adminSaveCategory(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        if (id == 0) {
            id = Db.insert("INSERT INTO t_product_category(name,parent_id,sort_order,icon,status) VALUES(?,?,?,?,?)", Web.string(b.get("name")), Web.intValue(b.get("parentId"), 0), Web.intValue(b.get("sortOrder"), 0), Web.string(b.get("icon")), Web.intValue(b.get("status"), 1));
        } else {
            Db.update("UPDATE t_product_category SET name=?,parent_id=?,sort_order=?,icon=?,status=? WHERE id=?", Web.string(b.get("name")), Web.intValue(b.get("parentId"), 0), Web.intValue(b.get("sortOrder"), 0), Web.string(b.get("icon")), Web.intValue(b.get("status"), 1), id);
        }
        Web.ok(response, Db.one("SELECT * FROM t_product_category WHERE id=?", id));
    }

    private void adminSaveAd(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        if (id == 0) {
            id = Db.insert("INSERT INTO t_advertisement(category_id,title,image_url,link_url,sort_order,status) VALUES(?,?,?,?,?,?)", Web.longValue(b.get("categoryId")), Web.string(b.get("title")), Web.string(b.get("imageUrl")), Web.string(b.get("linkUrl")), Web.intValue(b.get("sortOrder"), 0), Web.intValue(b.get("status"), 1));
        } else {
            Db.update("UPDATE t_advertisement SET category_id=?,title=?,image_url=?,link_url=?,sort_order=?,status=? WHERE id=?", Web.longValue(b.get("categoryId")), Web.string(b.get("title")), Web.string(b.get("imageUrl")), Web.string(b.get("linkUrl")), Web.intValue(b.get("sortOrder"), 0), Web.intValue(b.get("status"), 1), id);
        }
        Web.ok(response, Db.one("SELECT * FROM t_advertisement WHERE id=?", id));
    }

    private void createAdCategory(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        long id = Db.insert("INSERT INTO t_ad_category(name,status) VALUES(?,1)", Web.string(b.get("name")));
        Web.ok(response, Db.one("SELECT * FROM t_ad_category WHERE id=?", id));
    }

    private void adminOrderStatus(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        Db.update("UPDATE t_order SET status=?,updated_at=NOW() WHERE id=?", Web.intValue(b.get("status"), 0), id);
        Web.ok(response, true);
    }

    private void adminUserStatus(long id, HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Map<String, Object> b = Web.body(request);
        Db.update("UPDATE t_user SET status=?,updated_at=NOW() WHERE id=?", Web.intValue(b.get("status"), 1), id);
        Web.ok(response, true);
    }

    private void upload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        requireAdmin(request);
        Part file = request.getPart("file");
        String filename = System.currentTimeMillis() + "-" + Path.of(file.getSubmittedFileName()).getFileName();
        Path uploadDir = Path.of(getServletContext().getRealPath("/uploads"));
        Files.createDirectories(uploadDir);
        file.write(uploadDir.resolve(filename).toString());
        Web.ok(response, Map.of("url", request.getContextPath() + "/uploads/" + filename));
    }

    private void softDelete(String table, long id, HttpServletResponse response) throws Exception {
        Db.update("UPDATE " + table + " SET status=0 WHERE id=?", id);
        Web.ok(response, true);
    }

    private long requireUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new AuthException("请先登录");
        }
        return Web.longValue(session.getAttribute("userId"));
    }

    private void requireAdmin(HttpServletRequest request) {
        requireUser(request);
        Object role = request.getSession(false).getAttribute("role");
        if (!"ADMIN".equals(String.valueOf(role))) {
            throw new AuthException("需要管理员权限");
        }
    }

    private long pathId(String path) {
        long[] ids = pathIds(path);
        return ids[ids.length - 1];
    }

    private long[] pathIds(String path) {
        return Arrays.stream(path.split("/")).filter(s -> s.matches("\\d+")).mapToLong(Long::parseLong).toArray();
    }

    private static class AuthException extends RuntimeException {
        AuthException(String message) {
            super(message);
        }
    }
}
