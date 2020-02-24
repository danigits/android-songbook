package igrek.songbook.settings.preferences.sync;

import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.util.Collections;

import androidx.appcompat.app.AppCompatActivity;
import igrek.songbook.info.logger.Logger;
import igrek.songbook.info.logger.LoggerFactory;


public class QuickStart2 {
	public static final int REQUEST_CODE_SIGN_IN = 1;
	public static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
	
	private static DriveServiceHelper mDriveServiceHelper;
	private static String mOpenFileId;
	
	public static Logger logger = LoggerFactory.INSTANCE.getLogger();
	
	public static void test(AppCompatActivity activity) {
		requestSignIn(activity);
	}
	
	/**
	 * Starts a sign-in activity using {@link #REQUEST_CODE_SIGN_IN}.
	 */
	private static void requestSignIn(AppCompatActivity activity) {
		logger.debug("requesting login");
		GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestEmail()
				.requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
				.build();
		GoogleSignInClient client = GoogleSignIn.getClient(activity, signInOptions);
		
		// The result of the sign-in Intent is handled in onActivityResult.
		activity.startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);
	}
	
	/**
	 * Handles the {@code result} of a completed sign-in activity initiated from {@link
	 */
	public static void handleSignInResult(Intent result, AppCompatActivity activity) {
		GoogleSignIn.getSignedInAccountFromIntent(result).addOnSuccessListener(googleAccount -> {
			logger.debug("Signed in as " + googleAccount.getEmail());
			
			// Use the authenticated account to sign in to the Drive service.
			GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(activity, Collections
					.singleton(DriveScopes.DRIVE_FILE));
			credential.setSelectedAccount(googleAccount.getAccount());
			Drive googleDriveService = new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
					.setApplicationName("igrek.songbook")
					.build();
			
			// The DriveServiceHelper encapsulates all REST API and SAF functionality.
			// Its instantiation is required before handling any onClick actions.
			mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
		}).addOnFailureListener(exception -> logger.error("Unable to sign in.", exception));
	}
	
	/**
	 * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
	 */
	private static void openFilePicker(AppCompatActivity activity) {
		if (mDriveServiceHelper != null) {
			logger.debug("Opening file picker.");
			
			Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();
			
			// The result of the SAF Intent is handled in onActivityResult.
			activity.startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
		}
	}
	
	/**
	 * Opens a file from its {@code uri} returned from the Storage Access Framework file picker
	 */
	public static void openFileFromFilePicker(Uri uri, AppCompatActivity activity) {
		if (mDriveServiceHelper != null) {
			logger.debug("Opening " + uri.getPath());
			
			mDriveServiceHelper.openFileUsingStorageAccessFramework(activity.getContentResolver(), uri)
					.addOnSuccessListener(nameAndContent -> {
						String name = nameAndContent.first;
						String content = nameAndContent.second;
						
						logger.debug("name", name);
						logger.debug("content", content);
					})
					.addOnFailureListener(exception -> logger.error("Unable to open file from picker.", exception));
		}
	}
	
	/**
	 * Creates a new file via the Drive REST API.
	 */
	public static void createFile(AppCompatActivity activity) {
		if (mDriveServiceHelper != null) {
			logger.debug("Creating a file.");
			
			mDriveServiceHelper.createFile()
					.addOnSuccessListener(fileId -> readFile(fileId))
					.addOnFailureListener(exception -> logger.error("Couldn't create file.", exception));
		}
	}
	
	/**
	 * Retrieves the title and content of a file identified by {@code fileId} and populates the UI.
	 */
	public static void readFile(String fileId) {
		if (mDriveServiceHelper != null) {
			logger.debug("Reading file " + fileId);
			
			mDriveServiceHelper.readFile(fileId).addOnSuccessListener(nameAndContent -> {
				String name = nameAndContent.first;
				String content = nameAndContent.second;
				
				logger.debug("name", name);
				logger.debug("content", content);
				
				mOpenFileId = fileId;
			}).addOnFailureListener(exception -> logger.error("Couldn't read file.", exception));
		}
	}
	
	public static void readLastFile() {
		if (mDriveServiceHelper != null && mOpenFileId != null) {
			readFile(mOpenFileId);
		}
	}
	
	public static void save2() {
		if (mDriveServiceHelper != null) {
			mDriveServiceHelper.saveAppDataFile();
		}
	}
	
	/**
	 * Saves the currently opened file created via if one exists.
	 */
	public static void saveFile(AppCompatActivity activity) {
		if (mDriveServiceHelper != null && mOpenFileId != null) {
			logger.debug("Saving " + mOpenFileId);
			
			String fileName = "dupa";
			String fileContent = "dddd-upa";
			
			mDriveServiceHelper.saveFile(mOpenFileId, fileName, fileContent)
					.addOnFailureListener(exception -> logger.error("Unable to save file via REST.", exception));
		}
	}
	
	/**
	 * Queries the Drive REST API for files visible to this app and lists them in the content view.
	 */
	public static void query(AppCompatActivity activity) {
		if (mDriveServiceHelper != null) {
			logger.debug("Querying for files.");
			
			mDriveServiceHelper.queryFiles().addOnSuccessListener(fileList -> {
				StringBuilder builder = new StringBuilder();
				for (File file : fileList.getFiles()) {
					builder.append(file.getName()).append("\n");
				}
				String fileNames = builder.toString();
				
				logger.debug("fileNames", fileNames);
			}).addOnFailureListener(exception -> logger.error("Unable to query files.", exception));
		}
	}
	
	
}
