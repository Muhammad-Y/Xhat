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
 * i syftet att kunna registera sig som ny användare.
 * @author grupp09 Xhat
 */
public class RegisterNewUserPanel extends JPanel {
	private LogInController logInController;
	private JLabel lblHeadline = new JLabel("Register new user");
	private JPanel mainPanel = new JPanel(new BorderLayout());
	private JPanel contentPanel = new JPanel(new GridLayout(3, 2));
	private JPanel btnPanel = new JPanel();
	private JLabel lblUsername = new JLabel("Username: ");
	private JLabel lblPassword = new JLabel("Password: ");
	private JLabel lblPasswordRepeat = new JLabel("Repeat password: ");
	private JTextField txtUsername = new JTextField();
	private JPasswordField txtPassword = new JPasswordField();
	private JPasswordField txtPasswordRepeat = new JPasswordField();
	private JButton btnRegister = new JButton("Register");
	private JButton btnCancel = new JButton("Cancel");
	private Font font = new Font("SansSerif", Font.BOLD, 14);
	
	public RegisterNewUserPanel(LogInController logInController) {
		this.logInController = logInController;
		generatePanel();
	}

	/**
	 * genererar utseende för det fönster där en användare uppger 
	 * inloggningsuppgifter för att registrera sig som ny användare.
	 */
	public void generatePanel() {
		mainPanel.setPreferredSize(new Dimension(250, 150));
		lblHeadline.setPreferredSize(new Dimension(240, 30));
		lblHeadline.setFont(font);
		lblHeadline.setHorizontalAlignment(JLabel.CENTER);
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		txtPassword.setEchoChar('*');
		txtPasswordRepeat.setEchoChar('*');
		
		//Koppla knappar till actionListener
		ButtonListener listener = new ButtonListener();
		btnRegister.addActionListener(listener);
		btnCancel.addActionListener(listener);
		
		//lägger till olika komponenter för gränssnittet i olika paneler
		contentPanel.add(lblUsername);
		contentPanel.add(txtUsername);
		contentPanel.add(lblPassword);
		contentPanel.add(txtPassword);
		contentPanel.add(lblPasswordRepeat);
		contentPanel.add(txtPasswordRepeat);		
		mainPanel.add(lblHeadline, BorderLayout.NORTH);
		mainPanel.add(contentPanel, BorderLayout.CENTER);
		add(mainPanel);
		btnPanel.add(btnRegister);
		btnPanel.add(btnCancel);
		mainPanel.add(btnPanel, BorderLayout.SOUTH);
	}
	
	public String getUsername() {
		return txtUsername.getText();
	}
	
	public void setUsername(String string) {
		txtUsername.setText(string);
	}
	
	public String getPassword() {
		return String.valueOf(txtPassword.getPassword());
	}
	
	public void setPassword(String string) {
		txtPassword.setText(string);
	}
	
	public String getPasswordRepeat() {
		return String.valueOf(txtPasswordRepeat.getPassword());
	}
	
	public void setPasswordRepeat(String string) {
		txtPasswordRepeat.setText(string);
	}
	
	private class ButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == btnRegister) {
				logInController.register(getUsername(), getPassword(), getPasswordRepeat());
			} else if(e.getSource() == btnCancel) {
				logInController.showLogInPanel("", "");
			}
		}
	}

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.getRootPane(btnRegister).setDefaultButton(btnRegister);
    }

	// test --> kontrollera utseende för GUI
	public static void main(String[] args) {
		RegisterNewUserPanel panel = new RegisterNewUserPanel(null);
		JFrame frameLogIn = new JFrame("Sign in");
		frameLogIn.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frameLogIn.setResizable(false);
		frameLogIn.add(panel);
		frameLogIn.pack();
		frameLogIn.setLocationRelativeTo(null);
		frameLogIn.setVisible(true);
	}
}
