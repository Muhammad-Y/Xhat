package client.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


/**
 * En klass som möjliggör för en användare att interagera med systemet 
 * i syftet att kunna logga in.
 * @author grupp09 Xhat
 */
public class LogInPanel extends JPanel {
	private LogInController logInController;
	private JTextField txtUsername = new JTextField();
	private JPasswordField txtPassword = new JPasswordField();
	private JButton btnLogIn = new JButton("Sign in");
	private JButton btnRegister = new JButton("Register new user");
	private Font font = new Font("SansSerif", Font.BOLD, 14);

	public LogInPanel(LogInController logInController, String userName, String password) {
		this.txtUsername.setText(userName);
		this.txtPassword.setText(password);
		this.logInController = logInController;
		generatePanel(); //test
	}

	/**
	 * genererar utseende för det fönster där en användare väljer 
	 * att logga in.
	 */
	private void generatePanel() {
		//variabler som inte behöver nås utanför denna metod
		JLabel lblHeadline = new JLabel("Sign in");
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel contentPanel = new JPanel(new GridLayout(2,2));
		JPanel btnPanel = new JPanel(new GridLayout(2,1));
		JLabel lblUsername = new JLabel("Username: ");
		JLabel lblPassword = new JLabel("Password: ");
		txtPassword.setEchoChar('*');

		mainPanel.setPreferredSize(new Dimension(250, 150));
		lblHeadline.setPreferredSize(new Dimension(240, 30));
		lblHeadline.setFont(font);
		lblHeadline.setHorizontalAlignment(JLabel.CENTER);
		btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); //(top, left, bottom, right)

		//Koppla knappar till actionListener
		ButtonListener listener = new ButtonListener();
		btnLogIn.addActionListener(listener);
		btnRegister.addActionListener(listener);

		//lägger till olika komponenter för gränssnittet i olika paneler
		contentPanel.add(lblUsername);
		contentPanel.add(txtUsername);
		contentPanel.add(lblPassword);
		contentPanel.add(txtPassword);

		btnPanel.add(btnLogIn);
		btnPanel.add(btnRegister);

		mainPanel.add(lblHeadline, BorderLayout.NORTH);
		mainPanel.add(contentPanel, BorderLayout.CENTER);
		mainPanel.add(btnPanel, BorderLayout.SOUTH);
		add(mainPanel);	
	}

	/**
	 * returnerar det som användaren angivit som användarnamn
	 * @return innehållet i fältet för användarnamn
	 */
	public String getUsername() {
		return txtUsername.getText();
	}

	/**
	 * 
	 * @param string
	 */
	public void setUsername(String string) {
		txtUsername.setText(string);
	}

	/**
	 * 
	 * @return
	 */
	public String getPassword() {
		return String.valueOf(txtPassword.getPassword());
	}

	public void setPassword(String string) {
		//SwingUtilities
		txtPassword.setText(string);
	}

	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == btnLogIn) {
				logInController.login(getUsername(), getPassword());
			} else if (e.getSource() == btnRegister) {
				logInController.showRegisterNewUserPanel();
			}
		}
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		SwingUtilities.getRootPane(btnLogIn).setDefaultButton(btnLogIn);
	}

	//test --> kontrollera utseende för GUI
	public static void main(String[] args) {
		LogInPanel panel = new LogInPanel(null, "", "");
		JFrame frameLogIn = new JFrame("Sign in");
		frameLogIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameLogIn.setResizable(false);
		frameLogIn.add(panel);
		frameLogIn.pack();
		frameLogIn.setLocationRelativeTo(null);
		frameLogIn.setVisible(true);
	}
}
