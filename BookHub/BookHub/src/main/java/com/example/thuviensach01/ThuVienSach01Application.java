package com.example.thuviensach01;

import com.example.thuviensach01.entity.Role;
import com.example.thuviensach01.entity.User;
import com.example.thuviensach01.model.Book;
import com.example.thuviensach01.repository.BookRepository;
import com.example.thuviensach01.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class ThuVienSach01Application {

	public static void main(String[] args) {
		SpringApplication.run(ThuVienSach01Application.class, args);
	}

	/**
	 * Seed dữ liệu mặc định khi app khởi động:
	 * - Tạo user admin/admin123 nếu chưa có
	 * - Thêm vài cuốn sách mẫu nếu bảng book đang trống
	 */
	@Bean
	CommandLineRunner initData(UserRepository users,
							   BookRepository books,
							   PasswordEncoder encoder) {
		return args -> {
			// User mặc định
			if (!users.existsByUsername("admin")) {
				users.save(User.builder()
						.username("admin")
						.email("admin@example.com")
						.password(encoder.encode("admin123")) // BCrypt
						.role(Role.ADMIN)
						.build());
				System.out.println("✅ Đã tạo user mặc định: admin / admin123");
			}

			// Sách mẫu
			if (books.count() == 0) {
				books.save(new Book(null, "Clean Code", "Robert C. Martin", "Programming"));
				books.save(new Book(null, "Spring in Action", "Craig Walls", "Backend"));
				books.save(new Book(null, "Refactoring", "Martin Fowler", "Programming"));
				System.out.println("✅ Đã thêm dữ liệu sách mẫu");
			}
		};
	}
}
