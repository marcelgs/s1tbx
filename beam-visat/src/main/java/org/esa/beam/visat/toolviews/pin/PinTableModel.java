/*
 * $Id: PinTableModel.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package org.esa.beam.visat.toolviews.pin;

import com.bc.ceres.core.ProgressMonitor;
import com.jidesoft.validation.ValidationObject;
import com.jidesoft.validation.ValidationResult;
import com.jidesoft.validation.Validator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.PlacemarkDescriptor;
import org.esa.beam.util.math.MathUtils;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;

public class PinTableModel implements TableModel {

    public static final String[] DEFAULT_COLUMN_NAMES = new String[]{"X", "Y", "Lon", "Lat", "Label"};

    private final PlacemarkDescriptor placemarkDescriptor;
    private final Product product;
    private final Band[] selectedBands;
    private final TiePointGrid[] selectedGrids;

    public PinTableModel(PlacemarkDescriptor placemarkDescriptor, Product product, Band[] selectedBands,
                         TiePointGrid[] selectedGrids) {
        this.placemarkDescriptor = placemarkDescriptor;
        this.product = product;
        this.selectedBands = selectedBands;
        this.selectedGrids = selectedGrids;
    }

    public int getRowCount() {
        if (product != null) {
            return placemarkDescriptor.getPlacemarkGroup(product).getNodeCount();
        }
        return 0;
    }

    public int getColumnCount() {
        int count = DEFAULT_COLUMN_NAMES.length;
        if (selectedBands != null) {
            count += selectedBands.length;
        }
        if (selectedGrids != null) {
            count += selectedGrids.length;
        }
        return count;
    }

    public String getColumnName(int columnIndex) {
        if (columnIndex < DEFAULT_COLUMN_NAMES.length) {
            return DEFAULT_COLUMN_NAMES[columnIndex];
        }
        int newIndex = columnIndex - DEFAULT_COLUMN_NAMES.length;
        if (newIndex < getNumSelectedBands()) {
            return selectedBands[newIndex].getName();
        }
        newIndex -= getNumSelectedBands();
        if (selectedGrids != null && newIndex < selectedGrids.length) {
            return selectedGrids[newIndex].getName();
        }
        return "?";
    }

    public Class getColumnClass(int columnIndex) {
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < DEFAULT_COLUMN_NAMES.length;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (product != null) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            Pin pin = placemarkDescriptor.getPlacemarkGroup(product).get(rowIndex);

            if (columnIndex == 0) {
                return pin.getPixelPos() != null ? toText(pin.getPixelPos().x, 10.0f) : "n.a.";
            } else if (columnIndex == 1) {
                return pin.getPixelPos() != null ? toText(pin.getPixelPos().y, 10.0f) : "n.a.";
            } else if (columnIndex == 2) {
                return pin.getGeoPos() != null ? toText(pin.getGeoPos().lon, 1000.0f) : "n.a.";
            } else if (columnIndex == 3) {
                return pin.getGeoPos() != null ? toText(pin.getGeoPos().lat, 1000.0f) : "n.a.";
            } else if (columnIndex == 4) {
                return pin.getLabel();
            } else {
                int newIndex = columnIndex - 5;
                PixelPos pixelPos = pin.getPixelPos();
                if (pixelPos == null) {
                    return "No-data";
                }
                final int x = MathUtils.floorInt(pixelPos.getX());
                final int y = MathUtils.floorInt(pixelPos.getY());
                if (x < 0 || x >= width || y < 0 || y >= height) {
                    return "No-data";
                }
                if (newIndex < getNumSelectedBands()) {
                    final Band band = selectedBands[newIndex];
                    if (band.isPixelValid(x, y)) {
                        float[] value = null;
                        try {
                            value = band.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                            return String.valueOf(value[0]);
                        } catch (IOException e) {
                            return "I/O-error";
                        }
                    } else {
                        return "No-data";
                    }
                }
                newIndex -= getNumSelectedBands();
                if (selectedGrids != null && newIndex < selectedGrids.length) {
                    final TiePointGrid grid = selectedGrids[newIndex];
                    float[] value = null;
                    try {
                        value = grid.readPixels(x, y, 1, 1, value, ProgressMonitor.NULL);
                        return String.valueOf(value[0]);
                    } catch (IOException e) {
                        return "I/O-error";
                    }
                }
            }
        }
        return "";
    }

    private String toText(float x, float roundFactor) {
        return String.valueOf(MathUtils.round(x, roundFactor));
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
//        if (columnIndex < DEFAULT_COLUMN_NAMES.length) {
//            String strValue = (String) value;
//            Pin pin = placemarkDescriptor.getPlacemarkGroup(product).get(rowIndex);
//            if (columnIndex == 0) {
//                pin.setPixelPos(new PixelPos(Float.parseFloat(strValue), pin.getPixelPos().y));
//            } else if (columnIndex == 1) {
//                pin.setPixelPos(new PixelPos(pin.getPixelPos().x, Float.parseFloat(strValue)));
//            } else if (columnIndex == 2) {
//                pin.setGeoPos(new GeoPos(pin.getGeoPos().lat, Float.parseFloat(strValue)));
//            } else if (columnIndex == 3) {
//                pin.setGeoPos(new GeoPos(Float.parseFloat(strValue), pin.getGeoPos().lon));
//            } else if (columnIndex == 4) {
//                pin.setLabel(strValue);
//            } else {
//                throw new IllegalStateException("Not able to set value for column '" + columnIndex + "'");
//            }
//            // todo - catch NumberFormatException
//        }
    }

    public void addTableModelListener(TableModelListener l) {
    }

    public void removeTableModelListener(TableModelListener l) {
    }

    private int getNumSelectedBands() {
        return selectedBands != null ? selectedBands.length : 0;
    }

    public Validator getValidator(int columnIndex) {
        if (columnIndex < DEFAULT_COLUMN_NAMES.length) {
            if (columnIndex == 0) {
                return new ColumnValidator(new PixelXValidator());
            } else if (columnIndex == 1) {
                return new ColumnValidator(new PixelXValidator());
            } else if (columnIndex == 2) {
                return new ColumnValidator(new PixelXValidator());
            } else if (columnIndex == 3) {
                return new ColumnValidator(new PixelXValidator());
            } else if (columnIndex == 4) {
                return new ColumnValidator(ValidatorMethod.DEFAULT_METHOD);
            } else {
                throw new IllegalStateException("No validator found for column '" + columnIndex + "'");
            }
        }
        return new ColumnValidator(ValidatorMethod.DEFAULT_METHOD);
    }

    private static class ColumnValidator implements Validator {

        private ValidatorMethod method;

        public ColumnValidator(ValidatorMethod method) {
            this.method = method;
        }

        public ValidationResult validating(ValidationObject validationObject) {
            try {
                String newValue = (String) validationObject.getNewValue();
                return new ValidationResult(method.validate(newValue));
            } catch (Throwable t) {
                return new ValidationResult(false);
            }
        }
    }

    private interface ValidatorMethod {

        ValidatorMethod DEFAULT_METHOD = new ValidatorMethod() {
            public boolean validate(String value) {
                          return true;
            }
        };

        boolean validate(String value);
    }

    private class PixelXValidator implements ValidatorMethod {

        public boolean validate(String value) {

            try {
                Float.parseFloat(value);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }


}