/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.accounting.glaccount.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.regex.Pattern;
import org.apache.fineract.accounting.journalentry.data.JournalEntryAssociationParametersData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GLAccountReadPlatformServiceImplTest {

    private static final Pattern GROUP_BY_WITH_SORT_DIRECTION = Pattern.compile("(?i)group\\s+by\\s[^)]*\\b(asc|desc)\\b");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private GLAccountReadPlatformServiceImpl glAccountReadPlatformService;

    @Test
    void retrieveAllGLAccountsWithRunningBalanceDoesNotUseSortDirectionInGroupBy() {
        // Given
        JournalEntryAssociationParametersData associationParametersData = new JournalEntryAssociationParametersData(false, true);

        // When
        glAccountReadPlatformService.retrieveAllGLAccounts(null, null, null, null, null, associationParametersData);

        // Then
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any(Object[].class));
        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.contains("gl_j.organization_running_balance"), "Running balance column missing: " + executedSql);
        assertFalse(GROUP_BY_WITH_SORT_DIRECTION.matcher(executedSql).find(), "PostgreSQL rejects ASC/DESC in GROUP BY: " + executedSql);
        assertTrue(executedSql.contains("group by account_id, id)"), "Inner latest-entry subquery missing: " + executedSql);
        assertTrue(executedSql.contains("group by t2.account_id) t1)"), "Outer latest-entry subquery missing: " + executedSql);
    }
}
