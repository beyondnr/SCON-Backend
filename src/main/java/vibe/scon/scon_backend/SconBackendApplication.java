package vibe.scon.scon_backend;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class SconBackendApplication {

	public static void main(String[] args) {
		// .env 파일 로드 (로컬 개발 환경)
		loadDotenv();

		SpringApplication.run(SconBackendApplication.class, args);
	}

	/**
	 * .env 파일을 로드하여 시스템 프로퍼티로 설정합니다.
	 * Spring Boot가 환경변수로 인식할 수 있도록 System.setProperty()를 사용합니다.
	 * 
	 * <p>주의: .env 파일에는 KEY=VALUE 형식만 포함되어야 합니다.
	 * 긴 구분선(====)이나 특수 형식의 주석은 파싱 오류를 발생시킬 수 있습니다.
	 */
	private static void loadDotenv() {
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory("./")
					.filename(".env")
					.ignoreIfMissing()  // .env 파일이 없어도 에러 발생하지 않음
					.load();

			int loadedCount = 0;
			// .env 파일의 모든 변수를 시스템 프로퍼티로 설정
			for (var entry : dotenv.entries()) {
				try {
					String key = entry.getKey();
					String value = entry.getValue();
					if (value != null && !value.isEmpty() && !value.startsWith("your_")) {
						System.setProperty(key, value);
						log.debug("Loaded environment variable: {}", key);
						loadedCount++;
					}
				} catch (Exception e) {
					log.warn("Failed to load environment variable: {}", e.getMessage());
				}
			}

			if (loadedCount > 0) {
				log.info(".env file loaded successfully ({} variables)", loadedCount);
			} else {
				log.warn(".env file loaded but no valid variables found. Please check .env file format.");
			}
		} catch (DotenvException e) {
			// .env 파일 파싱 실패 시 상세 정보 출력
			log.error("Failed to parse .env file: {}", e.getMessage());
			log.warn("Please ensure .env file contains only KEY=VALUE format (avoid long separator lines like ====)");
			log.warn("Using system environment variables instead.");
		} catch (Exception e) {
			// 기타 예외 처리
			log.warn("Unexpected error loading .env file: {}. Using system environment variables instead.", e.getMessage());
		}
	}

}
