/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.HeadlessException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class BoundedJOptionPane extends JOptionPane {

	private static final long serialVersionUID = 1L;

	public static void showMessageDialog(
			Component parentComponent,
			String message,
			String title,
			int messageType) throws HeadlessException {
		String content;
		int screenWidth = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;
		int maxDialogWidth = screenWidth / 2;
		JLabel label = new JLabel(message);
		if (label.getPreferredSize().width > maxDialogWidth) {
			content = "<html><body><p style='width:" + maxDialogWidth + "px;'>" + message + "</p></body></html>";
		} else {
			content = "<html><body><p>" + message + "</p></body></html>";
		}
		JOptionPane.showMessageDialog(parentComponent, content, title, messageType);
	}

}
