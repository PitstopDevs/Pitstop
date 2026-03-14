package com.pitstop.app.Utilities;

import com.pitstop.app.utils.ProjectUtilities;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ProjectUtilitiesTest {

    @Test
    void checkModifiedUsername() {
        String username = "user_santo";

        String result = ProjectUtilities.getRealUsername(username);

        assertEquals("santo", result);
    }
}
