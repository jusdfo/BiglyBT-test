/*
 * Created on 05-Sep-2005
 * Created by Paul Gardner
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package com.biglybt.ui.swt.pifimpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.*;

import com.biglybt.ui.common.UIInstanceBase;
import com.biglybt.ui.swt.mainwindow.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Resource;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.download.DownloadManager;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.AERunnable;
import com.biglybt.core.util.AETemporaryFileHandler;
import com.biglybt.core.util.Debug;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.torrent.Torrent;
import com.biglybt.pif.ui.*;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.ui.model.BasicPluginViewModel;
import com.biglybt.pif.ui.tables.TableContextMenuItem;
import com.biglybt.pif.ui.toolbar.UIToolBarItem;
import com.biglybt.pif.ui.toolbar.UIToolBarManager;
import com.biglybt.pifimpl.local.PluginInitializer;
import com.biglybt.pifimpl.local.PluginInterfaceImpl;
import com.biglybt.pifimpl.local.download.DownloadImpl;
import com.biglybt.pifimpl.local.ui.UIManagerImpl;
import com.biglybt.pifimpl.local.ui.config.ConfigSectionRepository;
import com.biglybt.ui.IUIIntializer;
import com.biglybt.ui.mdi.MultipleDocumentInterface;
import com.biglybt.ui.swt.*;
import com.biglybt.ui.swt.components.shell.ShellFactory;
import com.biglybt.ui.swt.minibar.AllTransfersBar;
import com.biglybt.ui.swt.minibar.DownloadBar;
import com.biglybt.ui.swt.pif.*;
import com.biglybt.ui.swt.shells.MessageBoxShell;
import com.biglybt.ui.common.table.impl.TableContextMenuManager;
import com.biglybt.ui.swt.views.utils.ManagerUtils;

@SuppressWarnings("unused")
public class
UISWTInstanceImpl
	implements UIInstanceFactory, UISWTInstance, UIManagerEventListener, UIInstanceBase
{
	private Map<BasicPluginConfigModel,BasicPluginConfigImpl> 	config_view_map = new WeakHashMap<>();

	// Map<ParentId, Map<ViewId, Listener>>
	private Map<String,Map<String,UISWTViewEventListenerHolder>> views = new HashMap<>();

	private Map<PluginInterface,UIInstance>	plugin_map = new WeakHashMap<>();

	private boolean bUIAttaching;

	private final UIFunctionsSWT 		uiFunctions;

	public static interface SWTViewListener {
		public void setViewAdded(String parent, String id, UISWTViewEventListener l);
		public void setViewRemoved(String parent, String id, UISWTViewEventListener l);
	}

	private List<SWTViewListener> listSWTViewListeners = new ArrayList<>(0);

	public UISWTInstanceImpl() {
		// Since this is a UI **SWT** Instance Implementor, it's assumed
		// that the UI Functions are of UIFunctionsSWT
		uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
	}

	@Override
	public String
	getUIType()
	{
		return( UIT_SWT );
	}

	public void init(IUIIntializer init) {
		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();
		ui_manager.addUIEventListener(this);

		bUIAttaching = true;

		((UIManagerImpl) ui_manager).attachUI(this, init);

		bUIAttaching = false;
	}

	@Override
	public UIInstance
	getInstance(
		PluginInterface		plugin_interface )
	{
		UIInstance	instance = plugin_map.get( plugin_interface );

		if ( instance == null ){

			instance = new instanceWrapper( plugin_interface, uiFunctions, this );

			plugin_map.put( plugin_interface, instance );
		}

		return( instance );
	}

	@Override
	public boolean
	eventOccurred(
		final UIManagerEvent	event )
	{
		boolean	done = true;

		final Object	data = event.getData();

		switch( event.getType()){

			case UIManagerEvent.ET_SHOW_TEXT_MESSAGE:
			{
				Utils.execSWTThread(
					new Runnable()
					{
						@Override
						public void
						run()
						{
							String[]	params = (String[])data;

							new TextViewerWindow( params[0], params[1], params[2] );
						}
					});

				break;
			}
			case UIManagerEvent.ET_SHOW_MSG_BOX:
			{
				final int[] result = { UIManagerEvent.MT_NONE };

					Utils.execSWTThread(
					new Runnable()
					{
						@Override
						public void
						run()
						{
							UIFunctionsManagerSWT.getUIFunctionsSWT().bringToFront();

							Object[]	params = (Object[])data;

							long	_styles = ((Long)(params[2])).longValue();

							int		styles	= 0;
							int		def		= 0;

							if (( _styles & UIManagerEvent.MT_YES ) != 0 ){

								styles |= SWT.YES;
							}
							if (( _styles & UIManagerEvent.MT_YES_DEFAULT ) != 0 ){

								styles |= SWT.YES;
								def = SWT.YES;
							}
							if (( _styles & UIManagerEvent.MT_NO ) != 0 ){

								styles |= SWT.NO;
							}
							if (( _styles & UIManagerEvent.MT_NO_DEFAULT ) != 0 ){

								styles |= SWT.NO;
								def = SWT.NO;
							}
							if (( _styles & UIManagerEvent.MT_OK ) != 0 ){

								styles |= SWT.OK;
							}
							if (( _styles & UIManagerEvent.MT_OK_DEFAULT ) != 0 ){

								styles |= SWT.OK;
								def = SWT.OK;
							}

							if (( _styles & UIManagerEvent.MT_CANCEL ) != 0 ){

								styles |= SWT.CANCEL;
							}


							MessageBoxShell mb =
								new MessageBoxShell(
									styles,
									MessageText.getString((String)params[0]),
									MessageText.getString((String)params[1]));

							if ( def != 0 ){

								mb.setDefaultButtonUsingStyle( def );
							}

							if ( params.length == 4 && params[3] instanceof Map ){

								Map<String,Object>	options = (Map<String,Object>)params[3];

								String	rememberID 			= (String)options.get( UIManager.MB_PARAM_REMEMBER_ID );
								Boolean	rememberByDefault 	= (Boolean)options.get( UIManager.MB_PARAM_REMEMBER_BY_DEF );
								String	rememberText		= (String)options.get( UIManager.MB_PARAM_REMEMBER_RES );

								if ( rememberID != null && rememberByDefault != null && rememberText != null ){

									mb.setRemember( rememberID, rememberByDefault, rememberText );

									Number rememberIfOnlyButton		= (Number)options.get( UIManager.MB_PARAM_REMEMBER_IF_ONLY_BUTTON );
									if (rememberIfOnlyButton != null) {
										mb.setRememberOnlyIfButton(rememberIfOnlyButton.intValue());
									}
								}

								Number	auto_close_ms = (Number)options.get( UIManager.MB_PARAM_AUTO_CLOSE_MS );

								if ( auto_close_ms != null ){

									mb.setAutoCloseInMS( auto_close_ms.intValue());
								}
							}else if ( params.length >= 6 ){

								String	rememberID 			= (String)params[3];
								Boolean	rememberByDefault 	= (Boolean)params[4];
								String	rememberText		= (String)params[5];

								if ( rememberID != null && rememberByDefault != null && rememberText != null ){

									mb.setRemember( rememberID, rememberByDefault, rememberText );
								}
							}

							mb.open(null);

							int _r = mb.waitUntilClosed();

							int	r = 0;

							if (( _r & SWT.YES ) != 0 ){

								r |= UIManagerEvent.MT_YES;
							}
							if (( _r & SWT.NO ) != 0 ){

								r |= UIManagerEvent.MT_NO;
							}
							if (( _r & SWT.OK ) != 0 ){

								r |= UIManagerEvent.MT_OK;
							}
							if (( _r & SWT.CANCEL ) != 0 ){

								r |= UIManagerEvent.MT_CANCEL;
							}

							result[0] = r;
						}
					}, false );

				event.setResult( new Long( result[0] ));

				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_FILE:
			{
				TorrentOpener.openTorrent(((File)data).toString());

				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_TORRENT:
			{
				Torrent t = (Torrent)data;

				try{
					File f = AETemporaryFileHandler.createTempFile();

					t.writeToFile( f );

					TorrentOpener.openTorrent( f.toString());

				}catch( Throwable e ){

					Debug.printStackTrace( e );
				}

				break;
			}
			case UIManagerEvent.ET_OPEN_TORRENT_VIA_URL:
			{
				Display display = Utils.getDisplay();

				display.syncExec(new AERunnable() {
					@Override
					public void runSupport() {
						Object[] params = (Object[]) data;

						URL 		target 				= (URL) params[0];
						URL 		referrer 			= (URL) params[1];
						boolean 	auto_download 		= ((Boolean) params[2]).booleanValue();
						Map<?, ?>			request_properties	= (Map<?, ?>)params[3];

						// programmatic request to add a torrent, make sure az is visible

						if ( auto_download ){

							final Shell shell = uiFunctions.getMainShell();

							if ( shell != null ){

								final List<String>	alt_uris = new ArrayList<>();

								if ( request_properties != null ){

									request_properties = new HashMap(request_properties);

									for ( int i=1; i<16;i++){

										String key = "X-Alternative-URI-" + i;

										String uri = (String)request_properties.remove( key );

										if ( uri != null ){

											alt_uris.add( uri );

										}else{

											break;
										}
									}
								}

								final Map<?, ?> f_request_properties = request_properties;

								new FileDownloadWindow(
									shell,
									target.toString(),
									referrer == null ? null : referrer.toString(),
									request_properties,
									new Runnable()
									{
										int alt_index = 0;

										@Override
										public void run()
										{
											if ( alt_index < alt_uris.size()){

												String alt_target = alt_uris.get( alt_index++  );

												new FileDownloadWindow(
														shell,
														alt_target,
														null,
														f_request_properties,
														this );
											}
										}
									});
							}
						} else {

							// TODO: handle referrer?

							TorrentOpener.openTorrent(target.toString());
						}
					}
				});

				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;

					// property bundles can't handle spaces in keys
					//
					// If this behaviour changes, change the openView(model)
					// method lower down.
					String sViewID = model.getName().replaceAll(" ", ".");
					BasicPluginViewImpl view = new BasicPluginViewImpl(model);
					addView(UISWTInstance.VIEW_MAIN, sViewID, view);
				}

				break;
			}
			case UIManagerEvent.ET_PLUGIN_VIEW_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginViewModel ){
					BasicPluginViewModel model = (BasicPluginViewModel)data;
					// property bundles can't handle spaces in keys
					//
					// If this behaviour changes, change the openView(model)
					// method lower down.
					String sViewID = model.getName().replaceAll(" ", ".");
					removeViews(UISWTInstance.VIEW_MAIN, sViewID);
				}

				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_CREATED:
			{
				if ( data instanceof BasicPluginConfigModel ){

					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;

					BasicPluginConfigImpl view = new BasicPluginConfigImpl(new WeakReference<>(model));

					config_view_map.put( model, view );

			  	ConfigSectionRepository.getInstance().addConfigSection(view, model.getPluginInterface());
				}

				break;
			}
			case UIManagerEvent.ET_PLUGIN_CONFIG_MODEL_DESTROYED:
			{
				if ( data instanceof BasicPluginConfigModel ){

					BasicPluginConfigModel	model = (BasicPluginConfigModel)data;

					BasicPluginConfigImpl view = config_view_map.get( model );

					if ( view != null ){

				  	ConfigSectionRepository.getInstance().removeConfigSection(view);

					}
				}

				break;
			}
			case UIManagerEvent.ET_COPY_TO_CLIPBOARD:
			{
				ClipboardCopy.copyToClipBoard((String)data);

				break;
			}
			case UIManagerEvent.ET_OPEN_URL:
			{
				Utils.launch(((URL)data).toExternalForm());

				break;
			}
			case UIManagerEvent.ET_SHOW_CONFIG_SECTION: {
				event.setResult(Boolean.FALSE);

				if (!(data instanceof String)){
					break;
				}

				event.setResult(Boolean.TRUE);

				uiFunctions.getMDI().showEntryByID(
						MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG, data);

				break;
			}
			case UIManagerEvent.ET_FILE_OPEN: {
				File file_to_use = (File)data;
				Utils.launch(file_to_use.getAbsolutePath());
				break;
			}
			case UIManagerEvent.ET_FILE_SHOW: {
				File file_to_use = (File)data;
				final boolean use_open_containing_folder = COConfigurationManager.getBooleanParameter("MyTorrentsView.menu.show_parent_folder_enabled");
				ManagerUtils.open(file_to_use, use_open_containing_folder);
				break;
			}
			case UIManagerEvent.ET_HIDE_ALL: {
				boolean hide = (Boolean)data;

				uiFunctions.setHideAll( hide );

				break;
			}
			case UIManagerEvent.ET_HIDE_ALL_TOGGLE: {
				
				uiFunctions.setHideAll(!uiFunctions.getHideAll());

				break;
			}
			default:
			{
				done	= false;

				break;
			}
		}

		return( done );
	}

	@Override
	public Display
	getDisplay()
	{
		return Utils.getDisplay();
	}

	@Override
	public Image
	loadImage(
		String	resource )
	{
		throw( new RuntimeException( "plugin specific instance required" ));
	}

	@Override
	public UISWTGraphic
	createGraphic(
		Image img)
	{
		return new UISWTGraphicImpl(img);
	}

	@Override
	public Shell createShell(int style) {
		Shell shell = ShellFactory.createMainShell(style);
		Utils.setShellIcon(shell);
		return shell;
	}


	@Override
	public void
	detach()

		throws UIException
	{
		if (plugin_map != null) {
			List<UIInstance> values = new ArrayList<>(plugin_map.values());
			for (UIInstance uiInstance : values) {
				if (uiInstance instanceof instanceWrapper) {
					instanceWrapper iw = (instanceWrapper) uiInstance;
					iw.detach();
				}
			}
		}
	}

	@Override
	public void dispose() {
		List<PluginInterfaceImpl> pluginInterfaces = PluginInitializer.getPluginInterfaces();
		for (PluginInterfaceImpl pluginInterface : pluginInterfaces) {
			UIManager uiManager = pluginInterface.getUIManager();
			if (uiManager != null) {
				UIInstance[] uiInstances = uiManager.getUIInstances();
				for (UIInstance uiInstance : uiInstances) {
					if (uiInstance != this && (uiInstance instanceof UISWTInstance) && (uiInstance instanceof UIInstanceBase)) {
						((UIInstanceBase) uiInstance).unload(pluginInterface);
					}
				}
			}
		}


		UIManager ui_manager = PluginInitializer.getDefaultInterface().getUIManager();
		ui_manager.removeUIEventListener(this);

		SWTThread instance = SWTThread.getInstance();
		if (instance != null) {
			instance.terminateSWTOnly();
		}

		// views should be mostly empty, but still contains BasicPluginViewImpl
		// ..and bad plugins that didn't remove their views
		ArrayList<Map<String, UISWTViewEventListenerHolder>> holders = new ArrayList<>(views.values());
		views.clear();
		for (Map<String, UISWTViewEventListenerHolder> holder : holders) {
			for (UISWTViewEventListenerHolder eventListenerHolder : holder.values()) {

				if (!(eventListenerHolder.getListener() instanceof BasicPluginViewImpl)) {
					int numViews = eventListenerHolder.getNumViews();
					if (numViews == 0) {
						System.err.println("UISWTViewEventListener added but not removed: "
								+ eventListenerHolder.getCla() + "/ " + eventListenerHolder.getListener() + " for "
								+ eventListenerHolder.getViewID());
					} else {
						System.err.println(numViews + " view(s) not removed: "
								+ eventListenerHolder.getViewID());
					}
				}
				eventListenerHolder.dispose();

			}
		}
		plugin_map.clear();

	}

	/* (non-Javadoc)
		 * @see com.biglybt.ui.swt.pif.UISWTInstance#addView(java.lang.String, java.lang.String, java.lang.Class, java.lang.Object)
		 */
	@Override
	public void addView(String sParentID, String sViewID,
	                    Class<? extends UISWTViewEventListener> cla, Object datasource) {
		addView(null, sParentID, sViewID, cla, datasource);
	}

	public void addView(PluginInterface pi, String sParentID, String sViewID,
			Class<? extends UISWTViewEventListener> cla, Object datasource) {

		UISWTViewEventListenerHolder _l = new UISWTViewEventListenerHolder(sViewID,
				cla, datasource, pi);
		addView( sParentID, sViewID, _l );
	}

	/* (non-Javadoc)
	 * @see com.biglybt.ui.swt.pif.UISWTInstance#addView(java.lang.String, java.lang.String, com.biglybt.ui.swt.pif.UISWTViewEventListener)
	 */
	@Override
	public void addView(String sParentID, String sViewID,
	                    final UISWTViewEventListener l) {
		UISWTViewEventListenerHolder _l = new UISWTViewEventListenerHolder(sViewID, l, null );
		addView( sParentID, sViewID, _l );
	}

	public void addView( String sParentID,
			final String sViewID, final UISWTViewEventListenerHolder holder)
	{
		if (sParentID == null) {
			sParentID = UISWTInstance.VIEW_MAIN;
		}
		Map<String,UISWTViewEventListenerHolder> subViews = views.get(sParentID);
		if (subViews == null) {
			subViews = new LinkedHashMap<>();
			views.put(sParentID, subViews);
		}

		subViews.put(sViewID, holder );

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				@Override
				public void runSupport() {
					try {
						uiFunctions.addPluginView(sViewID, holder );
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}

		SWTViewListener[] viewListeners = listSWTViewListeners.toArray(new SWTViewListener[0]);
		for (SWTViewListener l : viewListeners) {
			l.setViewAdded(sParentID, sViewID, holder);
		}
	}

	public void addSWTViewListener(SWTViewListener l) {
		listSWTViewListeners.add(l);
	}

	public void removeSWTViewListener(SWTViewListener l) {
		listSWTViewListeners.remove(l);
	}

	// TODO: Remove views from PeersView, etc
	@Override
	public void removeViews(String sParentID, final String sViewID) {
		Map<String,UISWTViewEventListenerHolder> subViews = views.get(sParentID);
		if (subViews == null)
			return;

		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			Utils.execSWTThread(new AERunnable() {
				@Override
				public void runSupport() {
					try {
						if (uiFunctions != null) {
							uiFunctions.removePluginView(sViewID);
						}
					} catch (Throwable e) {
						// SWT not available prolly
					}
				}
			});
		}

		SWTViewListener[] viewListeners = listSWTViewListeners.toArray(new SWTViewListener[0]);
		for (UISWTViewEventListener holder : subViews.values()) {
  		for (SWTViewListener l : viewListeners) {
  			l.setViewRemoved(sParentID, sViewID, holder);
  		}
		}

		subViews.remove(sViewID);
	}

	@Override
	public boolean openView(final String sParentID, final String sViewID,
	                        final Object dataSource) {
		return openView(sParentID, sViewID, dataSource, true);
	}

	@Override
	public boolean openView(final String sParentID, final String sViewID,
	                        final Object dataSource, final boolean setfocus) {
		Map<String,UISWTViewEventListenerHolder> subViews = views.get(sParentID);
		if (subViews == null) {
			return false;
		}

		final UISWTViewEventListenerHolder l = subViews.get(sViewID);
		if (l == null) {
			return false;
		}

		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (uiFunctions != null) {
					uiFunctions.openPluginView(
							sParentID, sViewID, l, dataSource,
							setfocus && !bUIAttaching);
				}
			}
		});

		return true;
	}

	@Override
	public void openMainView(final String sViewID,
	                         UISWTViewEventListener l, Object dataSource) {
		openMainView(null,sViewID, l, dataSource, true);
	}

	public void openMainView(PluginInterface pi, String sViewID,
			UISWTViewEventListener l, Object dataSource) {
		openMainView( pi, sViewID, l, dataSource, true);
	}

	@Override
	public void openMainView(final String sViewID,
	                         final UISWTViewEventListener l, final Object dataSource,
	                         final boolean setfocus) {
		openMainView( null, sViewID, l, dataSource, setfocus );
	}
	public void openMainView(final PluginInterface pi, final String sViewID,
			final UISWTViewEventListener _l, final Object dataSource,
			final boolean setfocus) {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (uiFunctions != null) {
					UISWTViewEventListenerHolder l = new UISWTViewEventListenerHolder(sViewID, _l, pi );

					uiFunctions.openPluginView(UISWTInstance.VIEW_MAIN, sViewID, l, dataSource, setfocus && !bUIAttaching);
				}
			}
		});
	}

	@Override
	public UISWTView[] getOpenViews(String sParentID) {
		if (sParentID.equals(UISWTInstance.VIEW_MAIN)) {
			try {
				if (uiFunctions != null) {
					return uiFunctions.getPluginViews();
				}
			} catch (Throwable e) {
				// SWT not available prolly
			}
		}
		return new UISWTView[0];
	}

	// @see com.biglybt.pif.ui.UIInstance#promptUser(java.lang.String, java.lang.String, java.lang.String[], int)
	@Override
	public int promptUser(String title, String text, String[] options,
	                      int defaultOption) {

		MessageBoxShell mb = new MessageBoxShell(title, text, options,
				defaultOption);
		mb.open(null);
		// bah, no way to change this to use the UserPrompterResultListener trigger
		return mb.waitUntilClosed();
	}

	@Override
	public void showDownloadBar(Download download, final boolean display) {
		if (!(download instanceof DownloadImpl)) {return;}
		final DownloadManager dm = ((DownloadImpl)download).getDownload();
		if (dm == null) {return;} // Not expecting this, but just in case...
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (display) {
					DownloadBar.open(dm, getDisplay().getActiveShell());
				}
				else {
					DownloadBar.close(dm);
				}
			}
		}, false);
	}

	@Override
	public void showTransfersBar(final boolean display) {
		Utils.execSWTThread(new AERunnable() {
			@Override
			public void runSupport() {
				if (display) {
					AllTransfersBar.open(getDisplay().getActiveShell());
				}
				else {
					AllTransfersBar.closeAllTransfersBar();
				}
			}
		}, false);
	}

	// Core Functions
	// ==============

	@Override
	public UISWTViewEventListenerHolder[] getViewListeners(String sParentID) {
		Map<String, UISWTViewEventListenerHolder> map = views.get(sParentID);
		if (map == null) {
			return new UISWTViewEventListenerHolder[0];
		}
		UISWTViewEventListenerHolder[] array = map.values().toArray(new UISWTViewEventListenerHolder[0]);
		Arrays.sort(array, new Comparator<UISWTViewEventListenerHolder>() {
			@Override
			public int compare(UISWTViewEventListenerHolder o1,
			                   UISWTViewEventListenerHolder o2) {
				if ((o1.getPluginInterface() == null) && (o2.getPluginInterface() == null)) {
					return 0;
				}
				if ((o1.getPluginInterface() != null) && (o2.getPluginInterface() != null)) {
					return 0;
				}
				return o1.getPluginInterface() == null ? -1 : 1;
			}
		});
		return array;
	}

	// @see com.biglybt.pif.ui.UIInstance#getInputReceiver()
	@Override
	public UIInputReceiver getInputReceiver() {
		return new SimpleTextEntryWindow();
	}

	// @see com.biglybt.pif.ui.UIInstance#createMessage()
	@Override
	public UIMessage createMessage() {
		return new UIMessageImpl();
	}

	@Override
	public UISWTStatusEntry createStatusEntry() {
		final UISWTStatusEntryImpl entry = new UISWTStatusEntryImpl();
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if (functionsSWT == null) {
			Debug.outNoStack("No UIFunctionsSWT on createStatusEntry");
			return null;
		}

		IMainStatusBar mainStatusBar = functionsSWT.getMainStatusBar();
		if (mainStatusBar == null) {
			Debug.outNoStack("No MainStatusBar on createStatusEntry");
			return null;
		}
		mainStatusBar.createStatusEntry(entry);

		return entry;
	}

	@Override
	public boolean openView(BasicPluginViewModel model) {
		return openView(VIEW_MAIN, model.getName().replaceAll(" ", "."), null);
	}

	@Override
	public void openConfig(final BasicPluginConfigModel model) {
		Utils.execSWTThread(new Runnable() {
			@Override
			public void run() {
				uiFunctions.getMDI().loadEntryByID(
						MultipleDocumentInterface.SIDEBAR_SECTION_CONFIG, true, false,
						model.getSection());
			}
		});
	}

	@Override
	public UIToolBarManager getToolBarManager() {
		throw( new RuntimeException( "plugin specific instance required" ));
	}

	protected static class
	instanceWrapper
		implements UISWTInstance, UIToolBarManager, UIInstanceBase
	{
		private WeakReference<PluginInterface>		pi_ref;
		private UIFunctionsSWT						ui_functions;
		private UISWTInstanceImpl					delegate;

		private UIToolBarManagerCore 	toolBarManager;
		private List<UIToolBarItem> 	listItems			= new ArrayList<>();
		private List<Resource> 			listDisposeOnUnload = new ArrayList<>();

		protected
		instanceWrapper(
			PluginInterface			_pi,
			UIFunctionsSWT			_ui_functions,
			UISWTInstanceImpl		_delegate )
		{
			pi_ref			= new WeakReference<>(_pi );
			ui_functions	= _ui_functions;
			delegate		= _delegate;
		}

		@Override
		public UIToolBarItem getToolBarItem(String id) {
			return toolBarManager.getToolBarItem(id);
		}

		@Override
		public UIToolBarItem[] getAllToolBarItems() {
			return toolBarManager.getAllToolBarItems();
		}

		@Override
		public UIToolBarItem
		createToolBarItem(
			String id )
		{
			UIToolBarItem addToolBarItem = toolBarManager.createToolBarItem( id );

			synchronized( this ){

				listItems.add( addToolBarItem );
			}

			return addToolBarItem;
		}

		@Override
		public void addToolBarItem(UIToolBarItem item) {
			toolBarManager.addToolBarItem(item);
		}

		@Override
		public void removeToolBarItem(String id) {
			toolBarManager.removeToolBarItem(id);
		}

		public void
		detach()

			throws UIException
		{
			Utils.disposeSWTObjects(listDisposeOnUnload);

			listDisposeOnUnload.clear();
		}

		@Override
		public String
		getUIType()
		{
			return( delegate.getUIType());
		}

		@Override
		public Display
		getDisplay()
		{
			return( delegate.getDisplay());
		}

		@Override
		public Image
		loadImage(
			String	resource )
		{
			PluginInterface pi = pi_ref.get();

			if ( pi == null ){

				return( null );
			}

			InputStream is = pi.getPluginClassLoader().getResourceAsStream( resource);

			if ( is != null ){

				ImageData imageData = new ImageData(is);

				try {
					is.close();
				} catch (IOException e) {
					Debug.out(e);
				}

				Display display = getDisplay();

				Image image = new Image(display, imageData);

				image = Utils.adjustPXForDPI( display,  image );

				listDisposeOnUnload.add(image);

				return image;
			}

			return null;
		}

		@Override
		public UISWTGraphic
		createGraphic(
			Image img )
		{
			return( delegate.createGraphic( img ));
		}

		@Override
		public void
		addView(String sParentID, String sViewID, UISWTViewEventListener l)
		{
			PluginInterface pi = pi_ref.get();

			delegate.addView( sParentID, sViewID, new UISWTViewEventListenerHolder(sViewID, l, pi) );
		}

		@Override
		public void addView(String sParentID, String sViewID,
		                    Class<? extends UISWTViewEventListener> cla, Object datasource) {
			PluginInterface pi = pi_ref.get();

			delegate.addView(sParentID, sViewID, new UISWTViewEventListenerHolder(sViewID,
					cla, datasource, pi));
		}

		@Override
		public void
		openMainView(String sViewID, UISWTViewEventListener l,Object dataSource)
		{
			PluginInterface pi = pi_ref.get();

			delegate.openMainView( pi, sViewID, l, dataSource );
		}

		@Override
		public void
		openMainView(String sViewID, UISWTViewEventListener l,Object dataSource, boolean setfocus)
		{
			PluginInterface pi = pi_ref.get();

			delegate.openMainView( pi, sViewID, l, dataSource, setfocus );
		}


		@Override
		public void
		removeViews(String sParentID, String sViewID)
		{
			delegate.removeViews(sParentID, sViewID );
		}


		@Override
		public UISWTView[]
		getOpenViews(String sParentID)
		{
			return( delegate.getOpenViews(sParentID));
		}

		@Override
		public int promptUser(String title, String text, String[] options,
		                      int defaultOption) {
			return delegate.promptUser(title, text, options, defaultOption);
		}

		@Override
		public boolean openView(String sParentID, String sViewID, Object dataSource) {
			return delegate.openView(sParentID, sViewID, dataSource);
		}

		@Override
		public boolean openView(String sParentID, String sViewID, Object dataSource, boolean setfocus) {
			return delegate.openView(sParentID, sViewID, dataSource, setfocus);
		}

		@Override
		public UISWTViewEventListenerWrapper[] getViewListeners(String sParentId) {
			return delegate.getViewListeners(sParentId);
		}
		@Override
		public UIInputReceiver getInputReceiver() {
			return delegate.getInputReceiver();
		}

		@Override
		public UIMessage createMessage() {
			return delegate.createMessage();
		}

		@Override
		public void showDownloadBar(Download download, boolean display) {
			delegate.showDownloadBar(download, display);
		}

		@Override
		public void showTransfersBar(boolean display) {
			delegate.showTransfersBar(display);
		}

		@Override
		public UISWTStatusEntry createStatusEntry() {
			return delegate.createStatusEntry();
		}

		@Override
		public boolean openView(BasicPluginViewModel model) {
			return delegate.openView(model);
		}

		@Override
		public void openConfig(BasicPluginConfigModel model) {
			delegate.openConfig(model);
		}

		@Override
		public Shell createShell(int style) {
			return delegate.createShell(style);
		}

		@Override
		public UIToolBarManager
		getToolBarManager()
		{
			if ( toolBarManager == null ){

				UIToolBarManager tbm = ui_functions.getToolBarManager();

				if ( tbm instanceof UIToolBarManagerCore ){

					toolBarManager = (UIToolBarManagerCore)tbm;

				}else{

					return( null );
				}
			}

			return( this );
		}

		@Override
		public void
		unload(
			PluginInterface pi )
		{
			if ( toolBarManager != null ){

				synchronized( this ){

					for (UIToolBarItem item : listItems) {

						toolBarManager.removeToolBarItem(item.getID());
					}

					listItems.clear();
				}
			}

			Utils.disposeSWTObjects(listDisposeOnUnload);

			listDisposeOnUnload.clear();
		}
	}

	@Override
	public void
	unload(
		PluginInterface pi )
	{
		throw( new RuntimeException( "plugin specific instance required" ));
	}
}
