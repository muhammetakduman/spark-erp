package com.electrician.tracker.dto;

import java.util.List;

import com.electrician.tracker.domain.MaterialItem;

/** A group's figures and its lines (product name A–Z), for the grouped material list. */
public record MaterialGroup(MaterialGroupTotal total, List<MaterialItem> lines) {

    public String groupName() {
        return total.groupName();
    }
}
