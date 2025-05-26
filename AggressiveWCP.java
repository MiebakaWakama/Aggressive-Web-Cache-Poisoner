



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;


public class AggressiveWCP extends AbstractTableModel implements IBurpExtender, ITab, IMessageEditorController, IContextMenuFactory
{
	private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JSplitPane splitPane;
    private JPanel main_panel = new JPanel();
    private IMessageEditor requestViewer;
    private IMessageEditor responseViewer;
    private final List<LogEntry> log = new ArrayList<LogEntry>();
    private IHttpRequestResponse currentlyDisplayedItem;
    private JTextField header_txtfield = new JTextField(20);
    private int SN = 1;
    private int colorcount = 0;
    private Color[]colors = {new Color(141,119,97), new Color(185,123,95), new Color(202,111,36)};
    private int color_pos = 0;

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		return log.size();
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 3;
	}
	
	public Class<?> getColumnClass(int columnIndex)
	{
		return String.class;
        
	}
	
	public String getColumnName(int columnIndex) 
	{
		switch (columnIndex)
        {
            case 0:
                return "SN";
            case 1:
                return "Path";
            case 2:
            	return "Status";
            default:
                return "";
        }
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		LogEntry logEntry = log.get(rowIndex);

        switch (columnIndex)
        {
            case 0:
                return logEntry.SN;
            case 1:
                return logEntry.requestresponse.getUrl().getPath();
            case 2: 
            	return logEntry.requestresponse.getStatusCode();
            default:
                return "";
        }
	}

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation Invocation) 
	{
		// TODO Auto-generated method stub
		List<JMenuItem> menuitems = new ArrayList<JMenuItem>();
		JMenuItem aggWCP = new JMenuItem("Send to AggressiveWCP");
		
		aggWCP.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
						IHttpRequestResponse selrr = Invocation.getSelectedMessages()[0];
						//String host = selrr.getHost();
						//int port = selrr.getPort();
						
						String cache_buster = getAlphaNumericString(5);
						
						IHttpRequestResponse rr;

						for (int i = 1; i <= 18; i++ ) 
						{
							if (i < 10) 
							{
								byte[] injected_request = createPathConfusionRequest(selrr, "?x="+cache_buster, header_txtfield.getText());
								rr = callbacks.makeHttpRequest(selrr.getHttpService(), injected_request);
								
							}
							else
							{
								byte[] injected_request = createPathConfusionRequest(selrr, "?x="+cache_buster, null);
								rr = callbacks.makeHttpRequest(selrr.getHttpService(), injected_request);
							}
							
							LogEntry log_entry = new LogEntry(SN, callbacks.saveBuffersToTempFiles(rr));
				    	    int row = log.size();
					        log.add(log_entry);
					   
					    	fireTableRowsInserted(row,row);
					    	SN++;
						}
						
						}
						catch (Exception e) {
							PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
                   		     e.printStackTrace(stdout);
						}
					}
					
				});
				
				t.start();
				
			}
			
		});
		
		menuitems.add(aggWCP);
		
		return menuitems;
	}

	@Override
	public IHttpService getHttpService() {
		// TODO Auto-generated method stub
		return currentlyDisplayedItem.getHttpService();
	}

	@Override
	public byte[] getRequest() {
		// TODO Auto-generated method stub
		return currentlyDisplayedItem.getRequest();
	}

	@Override
	public byte[] getResponse() {
		// TODO Auto-generated method stub
		return currentlyDisplayedItem.getResponse();
	}

	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return "Aggressive WCP";
	}

	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return main_panel;
	}

	@Override
	public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) 
	{
		
		this.callbacks = callbacks;
		helpers = callbacks.getHelpers();
		
		callbacks.setExtensionName("Aggressive WCP");
		
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		//table of log entries
		Table logTable = new Table(AggressiveWCP.this);
        JScrollPane scrollPane = new JScrollPane(logTable);
        splitPane.setLeftComponent(scrollPane);
		
		requestViewer = callbacks.createMessageEditor(AggressiveWCP.this, false);
        responseViewer = callbacks.createMessageEditor(AggressiveWCP.this, false);
		JSplitPane bottom_splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, requestViewer.getComponent(), responseViewer.getComponent());
		splitPane.setRightComponent(bottom_splitpane);
		
		JPanel header_panel = new JPanel();
		header_panel.setLayout(new FlowLayout(FlowLayout.LEFT));
		header_panel.add(new JLabel("Header"));
		header_panel.add(header_txtfield);
		
		main_panel.setLayout(new BorderLayout());
		main_panel.add(header_panel, BorderLayout.NORTH);
		main_panel.add(splitPane, BorderLayout.CENTER);
		
		
		callbacks.customizeUiComponent(main_panel);
		callbacks.customizeUiComponent(splitPane);
		callbacks.customizeUiComponent(bottom_splitpane);
		callbacks.customizeUiComponent(scrollPane);
		callbacks.customizeUiComponent(logTable);
        
		// TODO Auto-generated method stub
		callbacks.addSuiteTab(AggressiveWCP.this);
        callbacks.registerContextMenuFactory(AggressiveWCP.this);
        callbacks.registerContextMenuFactory(AggressiveWCP.this);
	}
	
//create cache-busted request with header injected 
	  public byte[] createPathConfusionRequest(IHttpRequestResponse messageInfo, String added_text, String header) 
      {
		try {
      	String path = messageInfo.getUrl().getPath();
      	String message = new String(messageInfo.getRequest());
      	IRequestInfo requestinfo = helpers.analyzeRequest(messageInfo.getRequest());
      	String body = message.substring(requestinfo.getBodyOffset());
      	int path_offset = message.indexOf(path);
      	String before_path = message.substring(0,path_offset);
      	String after_path = message.substring(path_offset + path.length());
      	String new_path = path+added_text;
      	String new_message = before_path+new_path+after_path;
      	
      	if (header == null) 
      	{
      		return new_message.getBytes();
      	}
      	else {
      		List<String> headers = helpers.analyzeRequest(new_message.getBytes()).getHeaders();
      		headers.add(header);
      		return helpers.buildHttpMessage(headers, body.getBytes());
      		
      	}
		}
		catch(Exception e) {
			PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
  		     e.printStackTrace(stdout);
  		     return null;
		}
      }
	  
//Generate random string of given length	  
	  public String getAlphaNumericString(int n)
		 {
		 
		  // choose a Character random from this String
		  String AlphaNumericString = "abcdefghijklmnopqrstuvxyz";
		 
		  // create StringBuffer size of AlphaNumericString
		  StringBuilder sb = new StringBuilder(n);
		 
		  for (int i = 0; i < n; i++) {
		 
		   // generate a random number between
		   // 0 to AlphaNumericString variable length
		   int index
		    = (int)(AlphaNumericString.length()
		      * Math.random());
		 
		   // add Character one by one in end of sb
		   sb.append(AlphaNumericString
		      .charAt(index));
		  }
		 
		  return sb.toString();
		 }
	
	private class Table extends JTable 
	{
		public Table(TableModel tableModel) {
			
			super(tableModel);
			this.setDefaultRenderer(Integer.class, new ColorTableCellRenderer());
            this.setDefaultRenderer(String.class, new ColorTableCellRenderer());
		}
		
		public void changeSelection(int row, int col, boolean toggle, boolean extend)
        {
            // show the log entry for the selected row
			try {
            LogEntry logEntry = log.get(row);
            requestViewer.setMessage(logEntry.requestresponse.getRequest(), true);
            responseViewer.setMessage(logEntry.requestresponse.getResponse(), false);
            currentlyDisplayedItem = logEntry.requestresponse;
            
            super.changeSelection(row, col, toggle, extend);
			}
			catch(Exception e) {
				PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
	 		    e.printStackTrace();
			}
        }
	}
	
	private class ColorTableCellRenderer extends JPanel implements TableCellRenderer
    {
    	JLabel label = new JLabel();
    	
    	public Component getTableCellRendererComponent(JTable table, Object value,boolean isSelected, boolean hasFocus, int row, int column)
    	{
    		/**
    		colorcount++;
    		PrintWriter stdout = new PrintWriter(callbacks.getStdout(), true);
 		    stdout.println("This is color count: "+ colorcount);
    		if (colorcount%54 == 0) 
    		{
    			color_pos++;
    			if(color_pos == 3) {
    				color_pos = 0;
    			}
    			
    			label.setForeground(colors[color_pos]);
    		}
    		
    		else {*/
    			
    		//}
    		if (isSelected) {
        			setBackground(new Color(39,139,221));
        			label.setForeground(Color.white);
        		}
    		else {
    			label.setForeground(new Color(141,119,97));
    			setBackground(Color.white);
    		}
    		
    		label.setText(" "+value);
    		//label.setFont(new Font(label.getFont().getFamily(),Font.BOLD,label.getFont().getSize()));
    		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    		this.add(label);
    		setBorder(null);
    		//rs.close();
    		return this;
    	}
    }
	
	private class LogEntry
	{
		private final int SN;
		private final IHttpRequestResponsePersisted requestresponse;
		
		LogEntry(int sn, IHttpRequestResponsePersisted rr)
		{
			SN = sn;
			requestresponse = rr;
		}
	}
	
}
