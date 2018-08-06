package com.kdrag0n.bluestone.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Paginator {
    private int maxSize = 2000;
    private int count = 0;
    private List<String> currentPage = new LinkedList<>();
    private List<String> pages = new LinkedList<>();

    public Paginator() {}

    public Paginator(int maxSize) {
        this.maxSize = maxSize;
    }

    public void addLine(String line) {
        if (line.length() > maxSize - 1) {
            throw new IllegalArgumentException("Line exceeds maximum page size " + (maxSize - 1));
        }

        if (count + line.length() + 1 > maxSize) {
            closePage();
        }

        count += line.length() + 1;
        currentPage.add(line);
    }

    private void closePage() {
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

    public void reset() {
        currentPage.clear();
        pages.clear();
    }
}
