/*******************************************************************************
 * Copyright (c) 2013 WPI-Suite
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Team Rolling Thunder
 ******************************************************************************/

/**
 * @author Justin Hess
 * @author Christopher Botaish
 */
package edu.wpi.cs.wpisuitetng.modules.requirementmanager.view.requirements;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.wpi.cs.wpisuitetng.modules.requirementmanager.controller.UpdateRequirementController;
import edu.wpi.cs.wpisuitetng.modules.requirementmanager.models.Requirement;
import edu.wpi.cs.wpisuitetng.modules.requirementmanager.models.RequirementModel;
import edu.wpi.cs.wpisuitetng.modules.requirementmanager.view.ViewEventController;

public class RequirementSelector extends JScrollPane {
	private final Dimension buttonDimensions = new Dimension(125, 25);
	private JList<Requirement> requirementList;
	private List<JButton> buttonList;
	private JButton okButton;
	private JPanel buttonPanel;
	private RequirementSelectorMode mode;
	private Requirement activeRequirement;
	private RequirementSelectorListener listener;

	/**
	 * Constructor for the requirementselector
	 * @param listener the listener to report to
	 * @param requirement the requirement to fill the editor for
	 * @param mode the mode of the selector
	 * @param showBorder whether to show border or not
	 */
	public RequirementSelector(RequirementSelectorListener listener, Requirement requirement, RequirementSelectorMode mode, boolean showBorder) 
	{
		JPanel contentPanel = new JPanel();
		this.buttonList = new ArrayList<JButton>();
		this.listener = listener;
		this.activeRequirement = requirement;
		this.mode = mode;
		contentPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

		JScrollPane listScroll = new JScrollPane();
		listScroll.setPreferredSize(new Dimension(300, 125));
		requirementList = new JList<Requirement>();
		listScroll.setViewportView(requirementList);

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.PAGE_AXIS));
		contentPanel.add(listScroll);
		contentPanel.add(buttonPanel);

		String okText;
		if (this.mode == RequirementSelectorMode.POSSIBLE_CHILDREN) {
			okText = "Add Existing";
		} else {
			requirementList
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			okText = "Select Parent";
		}

		okButton = new JButton(okText);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				okPressed();
			}
		});
		okButton.setEnabled(false);
		this.addButton(0, okButton);

		if (showBorder)
			this.setBorder(BorderFactory.createLineBorder(Color.GRAY));

		this.refreshList();

		requirementList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				okButton.setEnabled(requirementList.getSelectedIndices().length > 0);
			}
		});

		requirementList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				clearSelection(e);
			}

			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				clearSelection(e);
			}

			private void clearSelection(MouseEvent e) {
				Point pClicked = e.getPoint();
				int index = requirementList.locationToIndex(pClicked);
				Rectangle rec = requirementList.getCellBounds(index, index);
				if (rec == null || !rec.contains(pClicked)) {
					requirementList.clearSelection();
				}
			}
		});
		this.setViewportView(contentPanel);
	}

	/**
	 * Adds a button to the requirement selector at the given index
	 * 
	 * @param index
	 *            the position of the new button
	 * @param button
	 *            the button to be added.
	 */
	public void addButton(int index, JButton button) {
		button.setMinimumSize(buttonDimensions);
		button.setPreferredSize(buttonDimensions);
		button.setMaximumSize(buttonDimensions);
		buttonList.add(index, button);

		buttonPanel.removeAll();
		for (JButton but : buttonList) {
			buttonPanel.add(but);
			buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		}
		buttonPanel.revalidate();
		buttonPanel.repaint();
	}

	/**
	 * Refreshes the requirement selector list.
	 */
	public void refreshList() {
		ListModel<Requirement> reqList = new DefaultListModel<Requirement>();

		switch (mode) {
		case POSSIBLE_CHILDREN:
			reqList = RequirementModel.getInstance().getPossibleChildren(
					activeRequirement);
			break;
		case POSSIBLE_PARENTS:
			reqList = RequirementModel.getInstance().getPossibleParents(
					activeRequirement);
			break;
		}

		requirementList.setModel(reqList);
	}

	/**
	 * Performs actions when the ok button is pressed.
	 */
	private void okPressed() {
		if (mode == RequirementSelectorMode.POSSIBLE_CHILDREN) {
			Object[] selectedList = requirementList.getSelectedValues();
			for (Object obj : selectedList) {
				Requirement newChild = (Requirement) obj;
				try {
					newChild.setParent(activeRequirement);
					ViewEventController.getInstance()
							.refreshEditRequirementPanel(newChild);
					UpdateRequirementController.getInstance()
							.updateRequirement(newChild);
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
			
			ViewEventController.getInstance().refreshEditRequirementPanel(activeRequirement);
		}
		else
		{
			Requirement parentRequirement = requirementList.getSelectedValue();
			try {
				activeRequirement.setParent(parentRequirement);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			UpdateRequirementController.getInstance().updateRequirement(
					activeRequirement);
		}

		listener.requirementSelected();
		this.refreshList();
	}

	/**
	 * Overrides the paintComponent method to retrieve the requirements on the
	 * first painting.
	 * 
	 * @param g
	 *            The component object to paint
	 */
	@Override
	public void paintComponent(Graphics g) {
		refreshList();

		super.paintComponent(g);
	}

	/**
	 * disable child panels
	 * 
	 * @param enabled whether
	 *            its enabled or not
	 */
	public void enableChildren(boolean enabled) {
		okButton.setEnabled(enabled);
		requirementList.setEnabled(enabled);
	}
}
