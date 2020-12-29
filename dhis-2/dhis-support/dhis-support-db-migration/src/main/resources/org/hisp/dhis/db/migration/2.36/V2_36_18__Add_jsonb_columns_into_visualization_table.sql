/*
 * Copyright (c) 2004-2020, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

-- This script is responsible for migrating and unifying existing axes and legend data into new JSONB columns.
-- See Feature DHIS2-10054.

-- What it does?
-- 1) creates new JSONB columns("legend" and "axes") into visualization table;
-- 2) runs a store procedure to move data/columns into the new JSONB columns("legend" and "axes").


-- Step 1) creates new JSONB columns("legend" and "axes") into visualization table;
alter table visualization add column if not exists "legend" jsonb;
alter table visualization add column if not exists "axes" jsonb;


-- Step 2) runs a store procedure to move data/columns into the new JSONB columns("legend" and "axes").
DO
$$
    DECLARE
        r record;
        legendJson json;
        axesJson json;
        axisIndex int;
        targetLineJson json;
        baseLineJson json;
        has_legend bool;
        has_range bool;
        has_domain bool;
        debug bool := FALSE;

    BEGIN
        FOR r IN (
            SELECT visualizationid, baseLineLabel, baseLineValue, domainAxisLabel,
                   rangeAxisDecimals, rangeAxisLabel, rangeAxisMaxValue, rangeAxisMinValue,
                   rangeAxisSteps, targetLineLabel, targetLineValue, subtitle, title, hideLegend,
                   fontStyle -> 'baseLineLabel' AS baseLineLabelFontStyle,
                   fontStyle -> 'targetLineLabel' AS targetLineLabelFontStyle,
                   fontStyle -> 'seriesAxisLabel' AS seriesAxisLabelFontStyle,
                   fontStyle -> 'verticalAxisTitle' AS verticalAxisTitleFontStyle,
                   fontStyle -> 'categoryAxisLabel' AS categoryAxisLabelFontStyle,
                   fontStyle -> 'horizontalAxisTitle' AS horizontalAxisTitleFontStyle,
                   fontStyle -> 'legend' AS legendFontStyle
            FROM visualization v
        ) LOOP
                -- Ensure global FLAGS are reset.
                axisIndex := 0;
                axesJson := '[{},{}]';
                targetLineJson := NULL;
                baseLineJson := NULL;
                has_legend := FALSE;
                has_range := FALSE;
                has_domain := FALSE;

                IF debug THEN
                    RAISE INFO '%','Migrating data for visualization id: ' || r.visualizationid;
                END IF;

                -- Migrate "legend"
                legendJson := '{}';

                IF r.legendFontStyle IS NOT NULL THEN
                    legendJson := jsonb_set(legendJson::jsonb, '{label}', format('{"fontStyle":%s}', r.legendFontStyle)::jsonb);
                    has_legend := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Legend:label: ', legendJson);
                    END IF;
                END IF;

                IF r.hideLegend IS NOT NULL THEN
                    legendJson := jsonb_set(legendJson::jsonb, '{hidden}'::TEXT[], r.hideLegend::TEXT::jsonb);
                    legendJson := json_strip_nulls(legendJson);
                    has_legend := TRUE;

                    IF debug THEN
                        RAISE INFO '%', 'Legend:hidden: ' || legendJson;
                    END IF;
                END IF;

                IF has_legend THEN
                    IF debug THEN
                        RAISE INFO '%', CONCAT('Updating legend column with: ', legendJson);
                    END IF;

                    UPDATE visualization SET legend = legendJson WHERE visualizationid = r.visualizationid;
                END IF;


                -- Migrate "axes"

                -- Axis RANGE
                IF r.seriesAxisLabelFontStyle IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',label}')::TEXT[], format('{"fontStyle":%s}', r.seriesAxisLabelFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, label: ', axesJson);
                    END IF;
                END IF;

                IF (COALESCE(r.rangeAxisLabel, '') != '' AND r.verticalAxisTitleFontStyle IS NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s"}', r.rangeAxisLabel, r.verticalAxisTitleFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, rangeAxisLabel: ', axesJson);
                    END IF;
                ELSEIF (COALESCE(r.rangeAxisLabel, '') = '' AND r.verticalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"fontStyle":%s}', r.rangeAxisLabel, r.verticalAxisTitleFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, verticalAxisTitleFontStyle: ', axesJson);
                    END IF;
                ELSEIF (COALESCE(r.rangeAxisLabel, '') != '' AND r.verticalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s", "fontStyle":%s}', r.rangeAxisLabel, r.verticalAxisTitleFontStyle)::jsonb);
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, rangeAxisLabel, verticalAxisTitleFontStyle: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisDecimals IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',decimals}')::TEXT[], to_jsonb(r.rangeAxisDecimals));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, decimals: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisMaxValue IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',maxValue}')::TEXT[], to_jsonb(r.rangeAxisMaxValue));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, maxValue: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisMinValue IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',minValue}')::TEXT[], to_jsonb(r.rangeAxisMinValue));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, minValue: ', axesJson);
                    END IF;
                END IF;

                IF r.rangeAxisSteps IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',steps}')::TEXT[], to_jsonb(r.rangeAxisSteps));
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, steps: ', axesJson);
                    END IF;
                END IF;

                IF (COALESCE(r.baseLineLabel, '') != '' AND r.baseLineLabelFontStyle IS NULL) THEN
                    baseLineJson := format('{"title": {"text":"%s"}}', r.baseLineLabel)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineLabel: ', baseLineJson);
                    END IF;
                ELSEIF (COALESCE(r.baseLineLabel, '') = '' AND r.baseLineLabelFontStyle IS NOT NULL) THEN
                    baseLineJson := format('{"title": {"fontStyle":%s}}', r.baseLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineLabelFontStyle: ', baseLineJson);
                    END IF;
                ELSEIF (COALESCE(r.baseLineLabel, '') != '' AND r.baseLineLabelFontStyle IS NOT NULL) THEN
                    baseLineJson := format('{"title": {"text":"%s", "fontStyle":%s}}', r.baseLineLabel, r.baseLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineLabel, baseLineLabelFontStyle: ', baseLineJson);
                    END IF;
                END IF;

                IF r.baseLineValue IS NOT NULL THEN
                    IF baseLineJson IS NOT NULL THEN
                        baseLineJson := jsonb_insert(baseLineJson::jsonb, ('{value}')::TEXT[], to_jsonb(r.baseLineValue));
                    ELSE
                        baseLineJson := format('{"value":%s}', r.baseLineValue)::jsonb;
                    END IF;

                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, baseLineValue: ', baseLineJson);
                    END IF;
                END IF;

                IF (COALESCE(r.targetLineLabel, '') != '' AND r.targetLineLabelFontStyle IS NULL) THEN
                    targetLineJson := format('{"title": {"text":"%s"}}', r.targetLineLabel)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineLabel: ', targetLineJson);
                    END IF;
                ELSEIF (COALESCE(r.targetLineLabel, '') = '' AND r.targetLineLabelFontStyle IS NOT NULL) THEN
                    targetLineJson := format('{"title": {"fontStyle":%s}}', r.targetLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineLabelFontStyle: ', targetLineJson);
                    END IF;
                ELSEIF (COALESCE(r.targetLineLabel, '') != '' AND r.targetLineLabelFontStyle IS NOT NULL) THEN
                    targetLineJson := format('{"title": {"text":"%s", "fontStyle":%s}}', r.targetLineLabel, r.targetLineLabelFontStyle)::jsonb;
                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineLabel, targetLineLabelFontStyle: ', targetLineJson);
                    END IF;
                END IF;

                IF r.targetLineValue IS NOT NULL THEN
                    IF targetLineJson IS NOT NULL THEN
                        targetLineJson := jsonb_insert(targetLineJson::jsonb, ('{value}')::TEXT[], to_jsonb(r.targetLineValue));
                    ELSE
                        targetLineJson := format('{"value":%s}', r.targetLineValue)::jsonb;
                    END IF;

                    has_range := TRUE;

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes RANGE, targetLineValue: ', targetLineJson);
                    END IF;
                END IF;

                IF has_range THEN
                    IF targetLineJson IS NOT NULL THEN
                        IF debug THEN
                            RAISE INFO '%', CONCAT('Target line body: ', targetLineJson);
                        END IF;

                        axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',targetLine}')::TEXT[], targetLineJson::jsonb);
                    END IF;

                    IF baseLineJson IS NOT NULL THEN
                        IF debug THEN
                            RAISE INFO '%', CONCAT('Base line body: ', baseLineJson);
                        END IF;

                        axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',baseLine}')::TEXT[], baseLineJson::jsonb);
                    END IF;

                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',index}')::TEXT[], to_jsonb(axisIndex));
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',type}')::TEXT[], to_jsonb('RANGE'::TEXT));

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes json RANGE: ', axesJson);
                    END IF;

                    axisIndex := axisIndex + 1;
                END IF;

                -- Axis DOMAIN
                IF r.categoryAxisLabelFontStyle IS NOT NULL THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',label}')::TEXT[], format('{"fontStyle":%s}', r.categoryAxisLabelFontStyle)::jsonb);
                    has_domain := TRUE;
                END IF;

                IF (COALESCE(r.domainAxisLabel, '') != '' AND r.horizontalAxisTitleFontStyle IS NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s"}', r.domainAxisLabel)::jsonb);
                    has_domain := TRUE;
                ELSEIF (COALESCE(r.domainAxisLabel, '') = '' AND r.horizontalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"fontStyle":%s}', r.horizontalAxisTitleFontStyle)::jsonb);
                    has_domain := TRUE;
                ELSEIF (COALESCE(r.domainAxisLabel, '') != '' AND r.horizontalAxisTitleFontStyle IS NOT NULL) THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',title}')::TEXT[], format('{"text":"%s", "fontStyle":%s}', r.domainAxisLabel, r.horizontalAxisTitleFontStyle)::jsonb);
                    has_domain := TRUE;
                END IF;

                IF has_domain THEN
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',index}')::TEXT[], to_jsonb(axisIndex));
                    axesJson := jsonb_set(axesJson::jsonb, ('{' || axisIndex || ',type}')::TEXT[], to_jsonb('DOMAIN'::TEXT));

                    IF debug THEN
                        RAISE INFO '%', CONCAT('Axes json DOMAIN: ', axesJson);
                    END IF;
                END IF;

                IF (has_domain OR has_range) THEN
                    IF axesJson IS NOT NULL AND axesJson::TEXT != '[{},{}]' THEN
                        -- It means we have some axis to persist.

                        axesJson := json_strip_nulls(axesJson);

                        -- Remove empty elements if any.
                        IF ((axesJson::json->0)::TEXT = '{}') THEN
                            axesJson := (axesJson::jsonb - 0);
                        END IF;

                        IF ((axesJson::json->1)::TEXT = '{}') THEN
                            axesJson := (axesJson::jsonb - 1);
                        END IF;

                        IF debug THEN
                            RAISE INFO '%', CONCAT('Updating axes column with: ', axesJson);
                        END IF;

                        UPDATE visualization SET axes = axesJson WHERE visualizationid = r.visualizationid;
                    END IF;
                END IF;
            END LOOP;
    END;
$$ LANGUAGE plpgsql
