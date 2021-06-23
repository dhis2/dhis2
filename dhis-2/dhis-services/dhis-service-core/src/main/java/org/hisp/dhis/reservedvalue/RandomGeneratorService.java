/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.reservedvalue;

import static org.hisp.dhis.util.Constants.RANDOM_GENERATION_CHUNK;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.Setter;

import org.springframework.stereotype.Service;

@Service
@Setter( AccessLevel.PROTECTED )
public class RandomGeneratorService implements Callable<List<String>>
{

    private final List<Character> lowercase = IntStream.range( 0, 26 ).mapToObj( ( n ) -> (char) (n + 'a') )
        .collect( Collectors.toList() );

    private final List<Character> uppercase = IntStream.range( 0, 26 ).mapToObj( ( n ) -> (char) (n + 'A') )
        .collect( Collectors.toList() );

    private String textPattern;

    @Override
    public List<String> call()
        throws Exception
    {

        LinkedList<String> patterns = new LinkedList<>();

        List<String> randomList = new ArrayList<>();

        Pattern randomPattern = Pattern.compile( "[X]+|[x]+|[#]+|[*]+" );
        Matcher matcher = randomPattern.matcher( textPattern );

        while ( matcher.find() )
        {
            patterns.add( textPattern.substring( matcher.start(), matcher.end() ) );
        }

        boolean isUpper = false;

        for ( int j = 0; j < RANDOM_GENERATION_CHUNK; j++ )
        {

            StringBuilder stringBuilder = new StringBuilder();

            for ( String pattern : patterns )
            {
                int i = 0;

                switch ( pattern.charAt( 0 ) )
                {
                case '*':

                    String randomUUIDForAll = UUID.randomUUID().toString();

                    while ( i < pattern.length() )
                    {

                        if ( Character.isLetter( randomUUIDForAll.charAt( i ) ) )
                        {

                            if ( isUpper )
                            {
                                stringBuilder.append( Character.toUpperCase( randomUUIDForAll.charAt( i ) ) );
                                isUpper = false;
                            }
                            else
                            {
                                stringBuilder.append( randomUUIDForAll.charAt( i ) );
                                isUpper = true;
                            }

                        }
                        else if ( Character.isDigit( randomUUIDForAll.charAt( i ) ) )
                        {
                            stringBuilder.append( randomUUIDForAll.charAt( i ) );
                        }

                        i++;
                    }

                    break;

                case '#':

                    stringBuilder.append( new BigInteger( 256, new SecureRandom() ).abs().toString(), 0, pattern.length() );
                    break;

                case 'X':
                    String randomUUIDForUpper = UUID.randomUUID().toString();

                    while ( i < pattern.length() )
                    {
                        if ( Character.isLetter( randomUUIDForUpper.charAt( i ) ) )
                        {
                            stringBuilder.append( Character.toUpperCase( randomUUIDForUpper.charAt( i ) ) );
                        }
                        else if ( Character.isDigit( randomUUIDForUpper.charAt( i ) ) )
                        {
                            stringBuilder
                                .append( uppercase.get( Character.getNumericValue( randomUUIDForUpper.charAt( i ) ) ) );
                        }

                        i++;
                    }

                    break;
                case 'x':
                    String randomUUIDForLower = UUID.randomUUID().toString();

                    while ( i < pattern.length() )
                    {
                        if ( Character.isLetter( randomUUIDForLower.charAt( i ) ) )
                        {
                            stringBuilder.append( randomUUIDForLower.charAt( i ) );
                        }
                        else if ( Character.isDigit( randomUUIDForLower.charAt( i ) ) )
                        {
                            stringBuilder
                                .append( lowercase.get( Character.getNumericValue( randomUUIDForLower.charAt( i ) ) ) );
                        }

                        i++;
                    }

                    break;
                }

            }

            randomList.add( stringBuilder.toString() );
        }

        return randomList;
    }
}
