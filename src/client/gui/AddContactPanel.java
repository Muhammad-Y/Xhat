package client.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class AddContactPanel extends JPanel {
	private MainController mainController;
	private JPanel mainPanel = new JPanel(new BorderLayout());
	private JPanel northPanel = new JPanel(new BorderLayout());
	private JPanel southPanel = new JPanel(new BorderLayout());
	private JPanel searchPanel = new JPanel(new GridLayout(1,2));
	private JPanel btnPanel = new JPanel(new GridLayout(1,2));
	private JLabel lblInfo = new JLabel("Add user to contact list");
	private JLabel lblSearch = new JLabel("Search user: ");
	private JTextField txtSearch = new JTextField();
	private JButton btnCancel = new JButton("Cancel");
	private JButton btnAdd = new JButton("Add");
	private JList<String> jlistSearchResult = new JList<String>();
	private JScrollPane spSearchResult = new JScrollPane(jlistSearchResult);
	private Font font = new Font("SansSerif", Font.BOLD, 14);
	
	public AddContactPanel(MainController mainController, DefaultListModel<String> resultsModel) {
		this.mainController = mainController;
		jlistSearchResult.setModel(resultsModel);
		txtSearch.getDocument().addDocumentListener(new ResultListener());
		generatePanel();
	}

	/**
	 * genererar utseende för det fönster där en användare lägger
	 * till en ny användare i sin kontaktlista.
	 */
	public void generatePanel() {
		mainPanel.setPreferredSize(new Dimension(200, 300));
		spSearchResult.setPreferredSize(new Dimension(200, 200));
		lblInfo.setPreferredSize(new Dimension(180, 30));
		lblInfo.setFont(font);
		lblInfo.setHorizontalAlignment(JLabel.CENTER);
		spSearchResult.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		searchPanel.add(lblSearch);
		searchPanel.add(txtSearch);
		
		btnPanel.add(btnAdd);
		btnPanel.add(btnCancel);
		northPanel.add(lblInfo, BorderLayout.NORTH);
		northPanel.add(searchPanel, BorderLayout.SOUTH);
		
		southPanel.add(spSearchResult, BorderLayout.NORTH);
		southPanel.add(btnPanel, BorderLayout.SOUTH);
		
		//SelectionMode
		jlistSearchResult.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		//Koppla knappar till actionListener
		ButtonListener listener = new ButtonListener();
		btnCancel.addActionListener(listener);
		btnAdd.addActionListener(listener);
		
		mainPanel.add(northPanel, BorderLayout.NORTH);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		add(mainPanel);
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
			if(e.getSource() == btnAdd) {
				String selectedUser = jlistSearchResult.getSelectedValue(); 
				if (selectedUser != null) {
					mainController.sendNewContactRequest(selectedUser, false);
					mainController.disposeFrameAddContact();
					JOptionPane.showMessageDialog(null, "A contact request has been sent to " + selectedUser);
				}
			} else if(e.getSource() == btnCancel) {
				mainController.disposeFrameAddContact();
			}
		}
	}
	
	private class ResultListener implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent e) {
			mainController.searchUser(getUserSearch());
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			mainController.searchUser(getUserSearch());
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			System.out.println("changedUpdate()");
		}
		
	}
	
	// test --> kontrollera utseende för GUI
//	public static void main(String[] args) {
//		AddContactPanel panel = new AddContactPanel(null);
//		JFrame frameAddContact = new JFrame("Add contact");
//		frameAddContact.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//		frameAddContact.setResizable(false);
//		frameAddContact.add(panel);
//		frameAddContact.pack();
//		frameAddContact.setLocationRelativeTo(null);
//		frameAddContact.setVisible(true);
//	}
}
