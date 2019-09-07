import React, { useState } from 'react';
import {navigate} from 'hookrouter'
import { BASE_URL } from '../../App';
import './Search.css';

function Search() {
    const [query, setQuery] = useState(null);

    function handleKeyDown(e) {
        if (e.key === 'Enter') {
            submitQuery();
        }
    }
    function submitQuery() {
        console.log("Query: " + query);
        navigate('/result', false, {q: query});

    }

    return (
        <div className="center-div">

            <div className="searchDiv">
                <div>
                    <span className=" bigfont">Search</span>
                </div>
                <div>
                    <input className="searchInput" type="text" name="search" placeholder="Type query here to search in crawled results"
                        onChange={e => setQuery(e.target.value)}
                        onKeyDown={e => handleKeyDown(e)} />
                </div>
                <div className="btndiv">
                    <input className="btn" type="button" value="Go" onClick={submitQuery} />
                </div>
            </div>
        </div>
    );
}

export default Search;








