package com.VSong.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PlayboardSeleniumService {

    private static final Logger logger = LoggerFactory.getLogger(PlayboardSeleniumService.class);

    public List<String> scrapeAllVtuberChannelIds() {
        Set<String> channelIds = new HashSet<>();
        WebDriver driver = null;

        try {
            WebDriverManager.chromedriver()
                    .clearDriverCache()
                    .setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            options.addArguments("--remote-allow-origins=*");

            driver = new ChromeDriver(options);

            // 1. 첫 번째 페이지 스크래핑
            driver.get("https://playboard.co/youtube-ranking/most-popular-v-tuber-channels-in-south-korea-daily");
            Set<String> page1Channels = scrapePage(driver, "첫 번째 페이지");
            channelIds.addAll(page1Channels);

            // 2. 다양한 URL 패턴 시도
            String[] additionalUrls = {
                    "https://playboard.co/youtube-ranking/most-popular-v-tuber-channels-in-south-korea-daily?page=2"
            };

            for (String url : additionalUrls) {
                try {
                    logger.info("URL 시도: {}", url);
                    driver.get(url);
                    Thread.sleep(3000);

                    Set<String> pageChannels = scrapePage(driver, url);
                    int newChannels = 0;
                    for (String channelId : pageChannels) {
                        if (!channelIds.contains(channelId)) {
                            channelIds.add(channelId);
                            newChannels++;
                        }
                    }

                    logger.info("URL에서 새로 발견한 채널: {}개", newChannels);

                } catch (Exception e) {
                    logger.warn("URL 실패: {} - {}", url, e.getMessage());
                }
            }

            // 3. 페이지네이션 버튼 클릭 시도
            driver.get("https://playboard.co/youtube-ranking/most-popular-v-tuber-channels-in-south-korea-daily");
            Thread.sleep(3000);

            for (int pageNum = 2; pageNum <= 3; pageNum++) {
                try {
                    if (clickNextPage(driver, pageNum)) {
                        Set<String> pageChannels = scrapePage(driver, "페이지 " + pageNum);
                        int newChannels = 0;
                        for (String channelId : pageChannels) {
                            if (!channelIds.contains(channelId)) {
                                channelIds.add(channelId);
                                newChannels++;
                            }
                        }
                        logger.info("페이지 {}에서 새로 발견한 채널: {}개", pageNum, newChannels);
                    } else {
                        logger.info("페이지 {}로 이동 실패", pageNum);
                        break;
                    }
                } catch (Exception e) {
                    logger.warn("페이지 {} 처리 실패: {}", pageNum, e.getMessage());
                    break;
                }
            }

            // 4. JavaScript로 더 많은 데이터 로드 시도
            try {
                driver.get("https://playboard.co/youtube-ranking/most-popular-v-tuber-channels-in-south-korea-daily");
                Thread.sleep(3000);

                Set<String> jsChannels = loadMoreWithJS(driver);
                int newChannels = 0;
                for (String channelId : jsChannels) {
                    if (!channelIds.contains(channelId)) {
                        channelIds.add(channelId);
                        newChannels++;
                    }
                }
                logger.info("JavaScript 로딩으로 새로 발견한 채널: {}개", newChannels);

            } catch (Exception e) {
                logger.warn("JavaScript 로딩 실패: {}", e.getMessage());
            }

        } catch (Exception e) {
            logger.error("전체 스크래핑 오류: {}", e.getMessage());
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        logger.info("최종 수집된 유니크 채널 ID 수: {}", channelIds.size());
        return new ArrayList<>(channelIds);
    }

    private Set<String> scrapePage(WebDriver driver, String pageName) {
        Set<String> pageChannelIds = new HashSet<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // 1. 더 긴 초기 대기
            Thread.sleep(10000);

            // 2. 페이지 끝까지 여러 번 반복 스크롤
            for (int cycle = 0; cycle < 3; cycle++) {
                logger.info("스크롤 사이클 {} 시작", cycle + 1);

                // 페이지 상단으로 이동
                js.executeScript("window.scrollTo(0, 0);");
                Thread.sleep(2000);

                // 천천히 끝까지 스크롤
                Long lastHeight = (Long) js.executeScript("return document.body.scrollHeight");

                while (true) {
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(3000); // 더 긴 대기

                    Long newHeight = (Long) js.executeScript("return document.body.scrollHeight");
                    if (newHeight.equals(lastHeight)) {
                        break;
                    }
                    lastHeight = newHeight;
                }

                // 중간 체크
                List<WebElement> currentLinks = driver.findElements(By.cssSelector("a[href*='/channel/UC']"));
                logger.info("사이클 {} 후 링크 수: {}", cycle + 1, currentLinks.size());
            }

            // 3. 최대한 많은 데이터 로드를 위한 JavaScript 실행
            js.executeScript(
                    "// 모든 lazy loading 트리거\n" +
                            "document.querySelectorAll('[data-src]').forEach(el => {\n" +
                            "  el.src = el.getAttribute('data-src');\n" +
                            "});\n" +
                            "\n" +
                            "// 모든 hidden 요소 표시 시도\n" +
                            "document.querySelectorAll('[style*=\"display: none\"]').forEach(el => {\n" +
                            "  el.style.display = 'block';\n" +
                            "});\n" +
                            "\n" +
                            "// 테이블 내 모든 행 강제 표시\n" +
                            "document.querySelectorAll('tr[style*=\"display: none\"]').forEach(el => {\n" +
                            "  el.style.display = 'table-row';\n" +
                            "});"
            );

            Thread.sleep(5000);

            // 4. 최종 링크 수집
            List<WebElement> allLinks = driver.findElements(By.cssSelector("a[href*='/channel/UC']"));
            logger.info("총 발견된 링크 수: {}", allLinks.size());

            Set<String> allHrefs = new HashSet<>();
            int validCount = 0;
            int invalidCount = 0;

            for (WebElement link : allLinks) {
                try {
                    String href = link.getAttribute("href");
                    allHrefs.add(href);

                    String channelId = extractChannelId(href);
                    if (channelId != null) {
                        pageChannelIds.add(channelId);
                        validCount++;
                    } else {
                        invalidCount++;
                        if (invalidCount <= 10) { // 처음 10개만 로그
                            logger.debug("유효하지 않은 링크: {}", href);
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            logger.info("총 href 수: {}, 유효한 채널 ID: {}, 무효한 링크: {}",
                    allHrefs.size(), validCount, invalidCount);

            logger.info("{}: {}개 채널 발견", pageName, pageChannelIds.size());

        } catch (Exception e) {
            logger.error("페이지 스크래핑 오류 ({}): {}", pageName, e.getMessage());
        }

        return pageChannelIds;
    }

    private boolean clickNextPage(WebDriver driver, int pageNum) {
        try {
            // 다양한 "다음" 버튼 셀렉터 시도
            String[] nextButtonSelectors = {
                    "a[contains(text(), '다음')]",
                    "a[contains(text(), 'Next')]",
                    "a[contains(text(), '>')]",
                    "button[contains(text(), '다음')]",
                    "button[contains(text(), 'Next')]",
                    ".pagination .next",
                    ".page-next",
                    "[data-page='" + pageNum + "']",
                    "a[href*='page=" + pageNum + "']"
            };

            for (String selector : nextButtonSelectors) {
                try {
                    List<WebElement> buttons = driver.findElements(By.cssSelector(selector));
                    if (!buttons.isEmpty()) {
                        WebElement button = buttons.get(0);
                        if (button.isDisplayed() && button.isEnabled()) {
                            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", button);
                            Thread.sleep(1000);
                            button.click();
                            Thread.sleep(3000);
                            logger.info("페이지 {} 버튼 클릭 성공", pageNum);
                            return true;
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }

            return false;

        } catch (Exception e) {
            logger.warn("페이지 {} 클릭 실패: {}", pageNum, e.getMessage());
            return false;
        }
    }

    private Set<String> loadMoreWithJS(WebDriver driver) {
        Set<String> jsChannelIds = new HashSet<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // JavaScript로 더 많은 데이터 로드 시도
            js.executeScript(
                    "if (window.loadMoreData) { window.loadMoreData(); } " +
                            "if (window.showMoreResults) { window.showMoreResults(); } " +
                            "if (window.loadAllData) { window.loadAllData(); }"
            );

            Thread.sleep(5000);

            // 극한 스크롤
            for (int i = 0; i < 50; i++) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                Thread.sleep(800);

                // 중간에 새로운 콘텐츠가 로드되었는지 확인
                if (i % 10 == 0) {
                    List<WebElement> currentLinks = driver.findElements(By.cssSelector("a[href*='/channel/UC']"));
                    logger.info("극한 스크롤 {}단계: {}개 링크", i, currentLinks.size());
                }
            }

            // 최종 링크 수집
            List<WebElement> allLinks = driver.findElements(By.cssSelector("a[href*='/channel/UC']"));

            for (WebElement link : allLinks) {
                try {
                    String href = link.getAttribute("href");
                    String channelId = extractChannelId(href);
                    if (channelId != null) {
                        jsChannelIds.add(channelId);
                    }
                } catch (Exception e) {
                    continue;
                }
            }

        } catch (Exception e) {
            logger.error("JavaScript 로딩 오류: {}", e.getMessage());
        }

        return jsChannelIds;
    }

    private String extractChannelId(String href) {
        try {
            if (href == null) {
                logger.debug("href가 null");
                return null;
            }

            if (!href.contains("/channel/")) {
                logger.debug("채널 링크가 아님: {}", href);
                return null;
            }

            int channelIndex = href.indexOf("/channel/");
            String channelPart = href.substring(channelIndex + 9);
            String channelId = channelPart.split("[?&#]")[0];

            if (!channelId.startsWith("UC")) {
                logger.debug("UC로 시작하지 않는 채널 ID: {}", channelId);
                return null;
            }

            if (channelId.length() != 24) {
                logger.debug("24자리가 아닌 채널 ID: {} (길이: {})", channelId, channelId.length());
                return null;
            }

            // 정상적인 채널 ID
            return channelId;

        } catch (Exception e) {
            logger.warn("채널 ID 추출 실패: {} - {}", href, e.getMessage());
            return null;
        }
    }
}