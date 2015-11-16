/*
 * This program is part of the OpenLMIS logistics management information
 * system platform software.
 *
 * Copyright © 2015 ThoughtWorks, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should
 * have received a copy of the GNU Affero General Public License along with
 * this program. If not, see http://www.gnu.org/licenses. For additional
 * information contact info@OpenLMIS.org
 */
package org.openlmis.core.view.fragment;

import android.app.AlertDialog;
import android.app.Dialog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestRunner;
import org.robolectric.util.FragmentTestUtil;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(LMISTestRunner.class)
public class BaseDialogFragmentTest {

    @Test
    public void shouldSetCallBack() {
        BaseDialogFragment fragment = BaseDialogFragment.newInstance(
                "title",
                "message",
                "btn_positive",
                "btn_negative",
                "onBackPressed");

        BaseDialogFragment.MsgDialogCallBack dialogCallBack = mock(BaseDialogFragment.MsgDialogCallBack.class);
        fragment.setCallBackListener(dialogCallBack);
        FragmentTestUtil.startFragment(fragment);

        Dialog dialog =

                fragment.getDialog();
        (((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)).performClick();
        verify(dialogCallBack).positiveClick(anyString());
    }
}