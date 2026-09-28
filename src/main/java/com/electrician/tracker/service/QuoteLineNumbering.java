package com.electrician.tracker.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.electrician.tracker.dto.QuoteLine;
import com.electrician.tracker.service.exception.ValidationException;

/**
 * The one place where quote line numbers ("Sıra No") are given, whatever
 * way a line was added (catalog product, free item, template). Lines are
 * only ever ordered by their number, never by name, brand or price; after
 * every change the numbers are 1..n without gaps. Every method returns a new
 * list and leaves its input untouched.
 */
public final class QuoteLineNumbering {

    /** A quote must fit on one PDF page. */
    public static final int MAX_LINES = 25;

    private QuoteLineNumbering() {
    }

    /** {@code line} at the end: number = highest so far + 1. */
    public static List<QuoteLine> append(List<QuoteLine> lines, QuoteLine line) {
        return appendAll(lines, List.of(line));
    }

    /** {@code added} below the existing lines, numbering on from the highest. */
    public static List<QuoteLine> appendAll(List<QuoteLine> lines, List<QuoteLine> added) {
        requireRoom(lines.size() + added.size());
        List<QuoteLine> result = new ArrayList<>(sorted(lines));
        int next = highest(result) + 1;
        for (QuoteLine line : added) {
            result.add(line.withLineNo(next++));
        }
        return renumber(result);
    }

    /** {@code line} placed at {@code index} (0-based), the lines from there on move down. */
    public static List<QuoteLine> insertAt(List<QuoteLine> lines, int index, QuoteLine line) {
        requireRoom(lines.size() + 1);
        List<QuoteLine> result = new ArrayList<>(sorted(lines));
        result.add(Math.max(0, Math.min(index, result.size())), line);
        return renumber(result);
    }

    /** The line at {@code index} replaced, keeping its number. */
    public static List<QuoteLine> replace(List<QuoteLine> lines, int index, QuoteLine line) {
        List<QuoteLine> result = new ArrayList<>(sorted(lines));
        result.set(index, line);
        return renumber(result);
    }

    /** The line at {@code index} swapped with the one above it; unchanged at the top. */
    public static List<QuoteLine> moveUp(List<QuoteLine> lines, int index) {
        return swap(lines, index, index - 1);
    }

    /** The line at {@code index} swapped with the one below it; unchanged at the bottom. */
    public static List<QuoteLine> moveDown(List<QuoteLine> lines, int index) {
        return swap(lines, index, index + 1);
    }

    public static List<QuoteLine> remove(List<QuoteLine> lines, int index) {
        List<QuoteLine> result = new ArrayList<>(sorted(lines));
        result.remove(index);
        return renumber(result);
    }

    /** Lines sorted by their current number and numbered 1..n. */
    public static List<QuoteLine> normalize(List<QuoteLine> lines) {
        return renumber(sorted(lines));
    }

    public static boolean canAdd(int lineCount) {
        return lineCount < MAX_LINES;
    }

    private static List<QuoteLine> swap(List<QuoteLine> lines, int index, int target) {
        List<QuoteLine> result = new ArrayList<>(sorted(lines));
        if (index < 0 || index >= result.size() || target < 0 || target >= result.size()) {
            return renumber(result);
        }
        QuoteLine moved = result.get(index);
        result.set(index, result.get(target));
        result.set(target, moved);
        return renumber(result);
    }

    /** Stable sort by number, so lines with equal numbers keep their list order. */
    private static List<QuoteLine> sorted(List<QuoteLine> lines) {
        return lines.stream().sorted(Comparator.comparingInt(QuoteLine::lineNo)).toList();
    }

    private static List<QuoteLine> renumber(List<QuoteLine> ordered) {
        List<QuoteLine> result = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            result.add(ordered.get(i).withLineNo(i + 1));
        }
        return List.copyOf(result);
    }

    private static int highest(List<QuoteLine> lines) {
        return lines.stream().mapToInt(QuoteLine::lineNo).max().orElse(0);
    }

    private static void requireRoom(int newCount) {
        if (newCount > MAX_LINES) {
            throw new ValidationException("error.quote.lines.tooMany");
        }
    }
}
