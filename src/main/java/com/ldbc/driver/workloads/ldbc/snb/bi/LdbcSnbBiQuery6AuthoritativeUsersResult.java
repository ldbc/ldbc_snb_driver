package com.ldbc.driver.workloads.ldbc.snb.bi;

public class LdbcSnbBiQuery6AuthoritativeUsersResult
{
    private final long personId;
    private final int authorityScore;

    public LdbcSnbBiQuery6AuthoritativeUsersResult( long personId, int authorityScore)
    {
        this.personId = personId;
        this.authorityScore = authorityScore;
    }

    public long personId()
    {
        return personId;
    }

    public int score()
    {
        return authorityScore;
    }

    @Override
    public String toString()
    {
        return "LdbcSnbBiQuery6AuthoritativeUsersResult{" +
               "personId=" + personId +
               ", authorityScore=" + authorityScore +
               '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        { return true; }
        if ( o == null || getClass() != o.getClass() )
        { return false; }

        LdbcSnbBiQuery6AuthoritativeUsersResult that = (LdbcSnbBiQuery6AuthoritativeUsersResult) o;

        if ( personId != that.personId )
        { return false; }
        return authorityScore == that.authorityScore;

    }

    @Override
    public int hashCode()
    {
        int result = (int) (personId ^ (personId >>> 32));
        result = 31 * result + authorityScore;
        return result;
    }
}
