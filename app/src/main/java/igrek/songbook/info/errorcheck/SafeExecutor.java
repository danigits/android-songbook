package igrek.songbook.info.errorcheck;


import javax.inject.Inject;

import igrek.songbook.dagger.DaggerIoc;
import igrek.songbook.info.logger.Logger;
import igrek.songbook.info.logger.LoggerFactory;
import igrek.songbook.info.UiInfoService;

public class SafeExecutor {
	
	@Inject
	UiInfoService uiInfoService;
	
	private Logger logger = LoggerFactory.getLogger();
	
	public void execute(Runnable action) {
		try {
			action.run();
		} catch (Throwable t) {
			logger.error(t);
			DaggerIoc.getFactoryComponent().inject(this);
			uiInfoService.showInfo("Error occurred: " + t.getMessage());
		}
	}
	
}