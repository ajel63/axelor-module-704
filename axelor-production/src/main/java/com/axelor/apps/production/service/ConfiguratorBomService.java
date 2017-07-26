/**
 * Axelor Business Solutions
 * <p>
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
 * <p>
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.production.service;

import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.production.db.ConfiguratorBOM;
import com.axelor.exception.AxelorException;
import com.axelor.rpc.JsonContext;

public interface ConfiguratorBomService {

    /**
     * Generate a bill of material from a configurator BOM
     * and a JsonContext holding the custom values
     * @param configuratorBOM
     * @param attributes
     * @param level
     */
    BillOfMaterial generateBillOfMaterial(ConfiguratorBOM configuratorBOM,
                                          JsonContext attributes, int level)
            throws AxelorException;
}
