plugins {
    `java-library`
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-mail")

    api(libs.konfigyr.mail.smtp)
    api(libs.konfigyr.mail.thymeleaf)

    api("org.springframework.boot:spring-boot-starter-thymeleaf")
    api("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")

    testImplementation(project(":konfigyr-test"))
    testImplementation("com.icegreen:greenmail")
    testImplementation("org.jsoup:jsoup")
}
