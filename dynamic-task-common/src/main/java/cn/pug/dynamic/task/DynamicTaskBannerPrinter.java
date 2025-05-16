package cn.pug.dynamic.task;

import cn.pug.dynamic.task.config.DynamicTaskProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
@Slf4j
public class DynamicTaskBannerPrinter implements InitializingBean {
    private final DynamicTaskProperties dtpProperties;

    public DynamicTaskBannerPrinter(DynamicTaskProperties dtpProperties) {
        this.dtpProperties = dtpProperties;
    }

    private static final String NAME = " :: Dynamic Task :: ";

    private static final String BANNER = "\n    ____                              _         ______           __  \n" +
            "   / __ \\__  ______  ____ _____ ___  (_)____   /_  __/___ ______/ /__\n" +
            "  / / / / / / / __ \\/ __ `/ __ `__ \\/ / ___/    / / / __ `/ ___/ //_/\n" +
            " / /_/ / /_/ / / / / /_/ / / / / / / / /__     / / / /_/ (__  ) ,<   \n" +
            "/_____/\\__, /_/ /_/\\__,_/_/ /_/ /_/_/\\___/    /_/  \\__,_/____/_/|_|  \n" +
            "      /____/                                                         ";

    @Override
    public void afterPropertiesSet() {
        if (!dtpProperties.isEnabledBanner()) {
            return;
        }
        log.info(AnsiOutput.toString(BANNER, "\n", AnsiColor.GREEN, NAME,
                AnsiColor.DEFAULT, AnsiStyle.FAINT));
    }
}
