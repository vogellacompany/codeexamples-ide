package com.vogella.nattable.parts;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;

/**
 * Converts a java.util.Date object to a given format and vice versa
 */
public class DefaultLocalDateDisplayConverter extends DisplayConverter {

	private DateTimeFormatter formatter;

	public DefaultLocalDateDisplayConverter() {
		formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    }

    @Override
    public Object canonicalToDisplayValue(Object canonicalValue) {
    	LocalDate date = (LocalDate) canonicalValue;
		return date.format(formatter);
    }

    @Override
    public Object displayToCanonicalValue(Object displayValue) {
        try {
			return LocalDate.parse(displayValue.toString());
        } catch (Exception e) {
			return LocalDate.now();
        }
    }

}
