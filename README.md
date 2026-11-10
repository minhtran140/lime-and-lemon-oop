# Lime & Lemon

Website bán quần áo làm cho đồ án môn Lập trình hướng đối tượng. Mã viết bằng Java 21 với Jakarta Servlet, JSP và JSTL, truy cập PostgreSQL 17 qua JDBC, đóng gói thành file WAR bằng Maven và chạy trên Tomcat 10.1. Chú thích trong mã Java, JSP, JavaScript, CSS và SQL viết bằng tiếng Việt và tập trung giải thích vì sao từng phần được thiết kế như vậy.

## Chức năng

Khách hàng xem trang chủ với danh mục, sản phẩm nổi bật và sản phẩm mới. Trang danh sách lọc được theo danh mục, tìm theo tên và sắp xếp theo giá. Mỗi sản phẩm có một màu, ba cỡ S/M/L và hiện số lượng còn trong kho. Khách thêm hàng vào giỏ, sửa số lượng hoặc xóa, rồi đặt hàng và trả tiền khi nhận hàng (COD). Thành viên đã đăng ký được giảm 5% và xem lại được lịch sử đơn.

Quản trị viên vào `/admin` để tạo, sửa, ngừng bán, đánh dấu nổi bật hoặc xóa sản phẩm, chỉnh tồn kho từng cỡ, thêm và xóa danh mục, xem đơn hàng và đổi trạng thái. Khi một đơn bị hủy, số hàng trong đơn được trả lại kho.

## Chạy bằng Docker

Cách này chỉ cần [Docker Desktop](https://www.docker.com/products/docker-desktop/), không phải cài Java, Maven, Tomcat hay PostgreSQL.

```bash
docker compose -p lime-and-lemon up --build -d --wait --wait-timeout 180
```

Lần build đầu mất vài phút. Xong thì mở http://127.0.0.1:8081/.

| Tài khoản demo | Email | Mật khẩu |
|---|---|---|
| Quản trị | `admin@limeandlemon.local` | `MacMocAdmin2026` |
| Khách hàng | tự đăng ký tại `/account/register` | |

| Việc | Lệnh |
|---|---|
| Dừng | `docker compose -p lime-and-lemon stop` |
| Xem log | `docker compose -p lime-and-lemon logs -f web db` |
| Xóa sạch dữ liệu demo | `docker compose -p lime-and-lemon down -v` |

Docker chỉ tạo database một lần, lúc volume còn trống: nạp `schema.sql` trước, `demo-catalog.sql` sau. Muốn làm lại từ đầu thì chạy `down -v`, nhưng lệnh này xóa toàn bộ dữ liệu demo.

Database dùng tài khoản `postgres`, mật khẩu `postgres`. Như vậy chỉ ổn khi chạy demo trên máy cá nhân.

Web chạy ở `127.0.0.1:8081`, PostgreSQL ở `127.0.0.1:5433`. Cổng 5432 được chừa lại cho PostgreSQL cài sẵn trên máy, nếu có.

### Trang chủ báo lỗi 500 sau khi cập nhật code

Lỗi này thường do volume cũ. Postgres chỉ chạy `schema.sql` khi volume còn trống, nên volume tạo từ phiên bản trước vẫn giữ schema và mật khẩu cũ. Dấu hiệu: `/products` hoặc `/` báo "Đã có lỗi xảy ra", trong khi `/account/login` và `/cart` vẫn mở được. Log của container cho biết nguyên nhân thật, ví dụ `password authentication failed`, `relation "product_sizes" does not exist` hoặc `function unaccent(...) does not exist`:

```powershell
docker compose -p lime-and-lemon logs --tail 100 web
```

Nếu không cần giữ dữ liệu trong volume, chạy `docker compose -p lime-and-lemon down -v` rồi `up --build -d --wait` để nạp lại (`down -v` xóa toàn bộ dữ liệu demo). Nếu cần giữ, chạy các migration trong `src/main/resources/db/migrations/`. File nào cũng chạy lại được nhiều lần. `20261027-product-webp.sql` đổi 13 ảnh PNG demo sang WebP và không đụng tới sản phẩm, đơn hàng hay ảnh tùy chỉnh. `20261031-unaccent-search.sql` bật extension `unaccent` để tìm "ao" ra "áo". `20261110-product-covers-rollback.sql` trả `image_url` từ thư mục `covers/` không tồn tại về ảnh có thật trên đĩa. `20261110-drop-demo-note.sql` xóa câu "Sản phẩm demo…" ở cuối mô tả sản phẩm.

```powershell
foreach ($file in 'migrations/20261027-product-webp.sql', 'migrations/20261031-unaccent-search.sql', 'migrations/20261110-product-covers-rollback.sql', 'migrations/20261110-drop-demo-note.sql') {
    Get-Content -Raw "src/main/resources/db/$file" | docker compose -p lime-and-lemon exec -T db psql -U postgres -d lime_and_lemon -v ON_ERROR_STOP=1
}
```

Lỗi phía server (SQL hoặc bug) được ghi vào log của container, người dùng chỉ thấy một câu thông báo chung. Vì vậy cứ xem `docker compose logs web` là biết lỗi gì.

## Chạy không dùng Docker

Cần JDK 21, Maven 3.9, Tomcat 10.1 và PostgreSQL 17 chạy ở cổng 5432 với tài khoản `postgres` / `postgres`.

1. Tạo database và nạp dữ liệu:

   ```bash
   psql -U postgres -c "CREATE DATABASE lime_and_lemon ENCODING 'UTF8'"
   psql -U postgres -d lime_and_lemon -f src/main/resources/db/schema.sql -f src/main/resources/db/demo-catalog.sql
   ```

2. Nếu tài khoản hoặc mật khẩu khác, sửa `src/main/webapp/META-INF/context.xml`, nơi khai báo connection pool mà Tomcat dùng. Test không đọc file này; chúng nhận `-Dtest.db.url`, `-Dtest.db.user` và `-Dtest.db.password`.
3. Chạy `mvn clean package`, chép `target/ROOT.war` vào thư mục `webapps` của Tomcat (xóa thư mục `ROOT` mặc định đi), khởi động Tomcat rồi mở `http://localhost:8080/`.

Nếu log Tomcat báo `invalid value for parameter "TimeZone": "Asia/Saigon"`, hãy chạy JVM ở múi giờ UTC rồi khởi động lại Tomcat (PowerShell: `$env:CATALINA_OPTS = '-Duser.timezone=UTC'`). Java trên Windows gọi múi giờ Việt Nam bằng tên cũ `Asia/Saigon` và driver JDBC gửi tên đó khi kết nối, nhưng PostgreSQL 17 trong image Docker (Debian) chỉ còn tên mới `Asia/Ho_Chi_Minh`. Giờ hiển thị vẫn đúng vì database lưu giờ UTC và `Order.getCreatedAt()` tự đổi sang giờ Việt Nam.

## Kiểm tra

```powershell
mvn clean package
java -ea -cp "target/classes;target/test-classes" com.clothingstore.OopDemo   # chạy riêng phần minh họa OOP
```

`mvn test` (đã nằm trong `package`, và Docker build cũng chạy) tự chạy các test không cần database, trong đó có `OopDemo`. `DatabaseTest` bị bỏ qua nếu chưa truyền `test.db.url`.

Muốn kiểm tra transaction, tồn kho, checkout và migration ảnh thì dựng một PostgreSQL tạm:

```powershell
docker run --rm -d --name lime-and-lemon-tests -e POSTGRES_PASSWORD=postgres -p 127.0.0.1:55439:5432 postgres:17
docker exec lime-and-lemon-tests pg_isready -U postgres
# Khi PostgreSQL đã sẵn sàng:
mvn "-Dtest.db.url=jdbc:postgresql://127.0.0.1:55439/postgres" "-Dtest.db.password=postgres" "-Duser.timezone=UTC" test
docker stop lime-and-lemon-tests
```

Mỗi test tự tạo một schema riêng rồi xóa đi, không đụng vào bảng có sẵn. Cổng 55439 dành riêng cho kiểm thử để không lẫn với database demo ở cổng 5433. Trên Windows cần `-Duser.timezone=UTC` vì lý do đã nói ở mục trên. Có ba test chạy nhiều luồng thật trên PostgreSQL để kiểm tra tranh chấp: hai admin cùng hủy một đơn, hai khách cùng mua món hàng cuối cùng, và một lượt mua chạy song song với một lượt hủy khóa các cỡ theo thứ tự ngược nhau.

## Bảo mật ở mức demo

- Mật khẩu được băm bằng SHA-256 không có salt (`Passwords`). Mức này đủ cho demo, không đủ cho cửa hàng thật.
- Khi đăng nhập và đăng ký, ứng dụng đổi session ID (`WebSupport.signIn`). Cookie session có `HttpOnly` và `SameSite=Lax`.
- Chống CSRF không dùng token. Mọi thao tác thay đổi dữ liệu đều đi qua `POST`, và `SameOriginFilter` trả về 403 cho `POST` mang `Origin` hoặc `Referer` của trang khác. Trình duyệt luôn gửi `Origin` khi `POST`, nên form đặt ở trang lạ bị chặn. Công cụ như `curl` không gửi header này và vẫn gọi được. Nếu sau này đặt site sau reverse proxy, proxy phải giữ nguyên header `Host`.

## Cấu trúc thư mục

```text
src/main/java/com/clothingstore/
  config/        khởi tạo ứng dụng, kết nối database
  domain/        model: user, product, cart, order, discount
  exception/     các loại lỗi nghiệp vụ
  repository/    toàn bộ câu SQL (JDBC)
  service/       nghiệp vụ
  web/           filter và servlet
src/main/resources/      db/schema.sql, db/demo-catalog.sql, db/migrations/
src/main/webapp/META-INF/context.xml   connection pool (Tomcat DBCP2)
src/main/webapp/         JSP (WEB-INF/views), CSS, JS và ảnh (assets)
src/test/java/           OopDemo, StoreRulesTest, DatabaseTest
```

Font Geist và Newsreader dùng giấy phép SIL Open Font License 1.1 (xem `src/main/webapp/assets/fonts/`).
