package com.khronodragon.bluestone;

import java.util.ArrayList;
import java.util.List;

public class Paginator {
    private int maxSize = 2000;
    private int count = 0;
    private List<String> currentPage = new ArrayList<>();
    private List<String> pages = new ArrayList<>();

    public Paginator() {}

    public Paginator(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addLine(String line) {
        if (line.length() > maxSize - 1) {
            throw new RuntimeException("Line exceeds maximum page size " + (maxSize - 1));
        }

        if (count + line.length() + 1 > maxSize) {
            closePage();
        }

        count += line.length() + 1;
        currentPage.add(line);
    }

    public void closePage() {
        pages.add(String.join("\n", currentPage));
        currentPage = new ArrayList<>();
        count = 0;
    }

    public List<String> getPages() {
        if (currentPage.size() > 0) {
            closePage();
        }
        return pages;
    }
}
