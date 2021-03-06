/**
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.applicationregistry.webui.component;

import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.webui.form.UIFormInputSet;

/**
 * Created by The eXo Platform SAS Author : tam.nguyen tamndrok@gmail.com May 16, 2009
 */
public class FormInputSetListAccess implements ListAccess<UIFormInputSet> {

    private final List<UIFormInputSet> list;

    FormInputSetListAccess(List<UIFormInputSet> list) {
        this.list = list;
    }

    public UIFormInputSet[] load(int index, int length) throws Exception {
        if (index < 0)
            throw new IllegalArgumentException("Illegal index: index must be a positive number");

        if (length < 0)
            throw new IllegalArgumentException("Illegal length: length must be a positive number");

        if (index + length > list.size())
            throw new IllegalArgumentException(
                    "Illegal index or length: sum of the index and the length cannot be greater than the list size");

        UIFormInputSet[] result = new UIFormInputSet[length];
        for (int i = 0; i < length; i++)
            result[i] = list.get(i + index);

        return result;
    }

    public int getSize() throws Exception {
        return list.size();
    }
}
