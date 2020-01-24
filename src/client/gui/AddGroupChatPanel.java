package client.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class AddGroupChatPanel extends JPanel {
	private MainController mainController;
	private JPanel mainPanel = new JPanel(new BorderLayout());
	private JPanel headlinePanel = new JPanel(new BorderLayout());
	private JPanel southPanel = new JPanel(new BorderLayout());
	private JPanel addedPanel = new JPanel(new BorderLayout());
	private JPanel addedMembersPanel = new JPanel(new BorderLayout());
	private JPanel northPanel = new JPanel(new GridLayout(2,2));
	private JPanel btnPanel = new JPanel(new GridLayout(1,2));
	private JLabel lblHeadline = new JLabel("Create a group chat");
	private JLabel lblSearch = new JLabel("Search user: ");
	private JLabel lblGroupChatName = new JLabel("Group name: ");
	private JLabel lblSearchResult = new JLabel("Search result");
	private JLabel lblAddedGroupMembers = new JLabel("Added group members");
	private JTextField txtGroupChatName = new JTextField();
	private JTextField txtSearch = new JTextField();
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnCreate = new JButton("Create");
	private JButton btnAdd = new JButton("Add to group");
	private DefaultListModel<String> membersModel = new DefaultListModel<String>();
	private JList<String> jlistSearchResult = new JList<String>();
	private JList<String> jlistAddedMembers = new JList<String>(membersModel);
	private JScrollPane spSearchResult = new JScrollPane(jlistSearchResult);
	private JScrollPane spAddedPersons = new JScrollPane(jlistAddedMembers);
	private Font font = new Font("SansSerif", Font.BOLD, 14);
	
	public AddGroupChatPanel(MainController mainController, DefaultListModel<String> resultsModel) {
		this.mainController = mainController;
		jlistSearchResult.setModel(resultsModel);
		txtSearch.getDocument().addDocumentListener(new ResultListener());
		generatePanel();
	}
	
	/**
	 * genererar utseende för det fönster där en användare
	 * skapar en ny gruppchatt.
	 */
	public void generatePanel() {
		mainPanel.setPreferredSize(new Dimension(220, 380));
		headlinePanel.setPreferredSize(new Dimension(200, 80));
		southPanel.setPreferredSize(new Dimension(200, 260));
		
		//
		addedPanel.setPreferredSize(new Dimension(200, 100));
		
		//
		addedMembersPanel.setPreferredSize(new Dimension(200, 100));
		
		//
		spSearchResult.setPreferredSize(new Dimension(200, 80));
		
		spAddedPersons.setPreferredSize(new Dimension(180, 95));
		lblAddedGroupMembers.setPreferredSize(new Dimension(180, 40));
		lblHeadline.setPreferredSize(new Dimension(180, 30));
		lblHeadline.setFont(font);
		lblHeadline.setHorizontalAlignment(JLabel.CENTER);
		spSearchResult.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		spAddedPersons.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		headlinePanel.add(lblHeadline, BorderLayout.NORTH);
		headlinePanel.add(northPanel, BorderLayout.SOUTH);
		
		northPanel.add(lblGroupChatName);
		northPanel.add(txtGroupChatName);
		northPanel.add(lblSearch);
		northPanel.add(txtSearch);
		
		addedPanel.add(lblSearchResult, BorderLayout.NORTH);
		addedPanel.add(spSearchResult, BorderLayout.CENTER);
		addedPanel.add(btnAdd, BorderLayout.SOUTH);
		
		addedMembersPanel.add(lblAddedGroupMembers, BorderLayout.NORTH);
		addedMembersPanel.add(spAddedPersons, BorderLayout.SOUTH);
		
		btnPanel.add(btnCreate);
		btnPanel.add(btnCancel);
		
		southPanel.add(addedPanel, BorderLayout.NORTH);
		southPanel.add(addedMembersPanel, BorderLayout.CENTER);
		southPanel.add(btnPanel, BorderLayout.SOUTH);
		
		//SelectionMode
		jlistSearchResult.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jlistAddedMembers.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		//Koppla knappar till actionListener
		ButtonListener listener = new ButtonListener();
		btnCancel.addActionListener(listener);
		btnCreate.addActionListener(listener);
		btnAdd.addActionListener(listener);
		
		mainPanel.add(headlinePanel, BorderLayout.NORTH);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		add(mainPanel);
	}
	
	public String getGroupChatName() {
		return txtGroupChatName.getText();
	}
	
	public void setGroupChatName(String string) {
		txtGroupChatName.setText(string);
	}
	
	public String getUserSearch() {
		return txtSearch.getText();
	}
	
	public void setUserSearch(String string) {
		txtSearch.setText(string);
	}
	
	//behöver metod för att hämta värdet för innehållet i resultatet från sökningen
	
	//behöver metod för att sätta värdet för innehållet i resultatet från sökningen

	//lägga till funktion för vad som ska hända om man trycker på knappar
	
	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == btnAdd) {
				String selectedContact = jlistSearchResult.getSelectedValue();
				if (selectedContact != null && !membersModel.contains(selectedContact)) {
					membersModel.addElement(selectedContact);
				}
			} else if(e.getSource() == btnCreate) {
				String groupName = getGroupChatName();
				mainController.createNewGroup(groupName, membersModel);
			} else if(e.getSource() == btnCancel) {
				mainController.disposeFrameAddGroup();
			}
		}
	}
	
	private class ResultListener implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) {
			mainController.searchContact(getUserSearch());
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			mainController.searchContact(getUserSearch());
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			System.out.println("changedUpdate()");
		}
		
	}
	
	// test --> kontrollera utseende för GUI
	public static void main(String[] args) {
		AddGroupChatPanel panel = new AddGroupChatPanel(null,new DefaultListModel<String>());
		JFrame frameAddGroup = new JFrame("Add group chat");
		frameAddGroup.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frameAddGroup.setResizable(false);
		frameAddGroup.add(panel);
		frameAddGroup.pack();
		frameAddGroup.setLocationRelativeTo(null); 
		frameAddGroup.setVisible(true);
	}
}
