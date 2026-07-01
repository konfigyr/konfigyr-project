plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-mail")

    api("com.konfigyr:konfigyr-mail-smtp:1.0.0")
    api("com.konfigyr:konfigyr-mail-thymeleaf:1.0.0")

    api("org.springframework.boot:spring-boot-starter-thymeleaf")
    api("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")

    testImplementation(project(":konfigyr-test"))
    testImplementation("com.icegreen:greenmail")
    testImplementation("org.jsoup:jsoup")
}
