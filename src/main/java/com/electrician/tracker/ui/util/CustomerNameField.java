package com.electrician.tracker.ui.util;

import java.util.List;
import java.util.function.Consumer;

import com.electrician.tracker.domain.Customer;
import com.electrician.tracker.service.CustomerService;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.ComboBox;

/**
 * An editable customer field: listed customers are suggested while typing,
 * and any other name can be typed freely. When the typed name is a listed
 * customer (ignoring case, Turkish letters and spaces) that customer is the
 * {@link #matchedCustomerProperty() match}; picking or typing a customer
 * reports it once so the form can fill in address, phone and e-mail.
 */
public final class CustomerNameField {

    private final ComboBox<String> comboBox;
    private final SuggestionPicker picker;
    private final ReadOnlyObjectWrapper<Customer> matched = new ReadOnlyObjectWrapper<>();
    private List<Customer> customers;
    private Consumer<Customer> onCustomerChosen = customer -> {
    };
    private boolean silent;

    public CustomerNameField(ComboBox<String> comboBox, List<Customer> customers) {
        this.comboBox = comboBox;
        this.customers = List.copyOf(customers);
        this.picker = new SuggestionPicker(comboBox, names(customers));
        comboBox.getEditor().textProperty().addListener((obs, old, text) -> refreshMatch(text));
    }

    /** Called with the customer when the typed or picked name becomes a listed customer. */
    public void setOnCustomerChosen(Consumer<Customer> action) {
        this.onCustomerChosen = action;
    }

    public ReadOnlyObjectProperty<Customer> matchedCustomerProperty() {
        return matched.getReadOnlyProperty();
    }

    public Customer matchedCustomer() {
        return matched.get();
    }

    public String typedName() {
        return picker.typedText();
    }

    /** Shows a saved name without filling the other fields from the customer list. */
    public void showName(String name) {
        silent = true;
        try {
            picker.select(name);
            refreshMatch(name);
        } finally {
            silent = false;
        }
    }

    /** After a new customer was added elsewhere (e.g. while saving). */
    public void reload(List<Customer> newCustomers) {
        this.customers = List.copyOf(newCustomers);
        names(newCustomers).forEach(picker::remember);
        silent = true;
        try {
            refreshMatch(comboBox.getEditor().getText());
        } finally {
            silent = false;
        }
    }

    private void refreshMatch(String text) {
        Customer found = CustomerService.findByName(customers, text).orElse(null);
        Customer previous = matched.get();
        matched.set(found);
        if (!silent && found != null && (previous == null || !previous.getId().equals(found.getId()))) {
            onCustomerChosen.accept(found);
        }
    }

    private static List<String> names(List<Customer> customers) {
        return customers.stream().map(Customer::getName).toList();
    }
}
