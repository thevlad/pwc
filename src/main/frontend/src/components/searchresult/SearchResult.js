import React, { useState, useRef, useEffect } from 'react';
import { Button } from '@material-ui/core';
import './SearchResult.css';
import { BASE_URL } from '../../App';
import { withRouter, Link } from 'react-router-dom';
import Typography from '@material-ui/core/Typography';
import InputWithTitleAndSubmit from '../common/InputWithTitleAndSubmit';
import { default as UILink } from '@material-ui/core/Link';
import { makeStyles } from '@material-ui/core/styles';
import TextField from '@material-ui/core/TextField';

const SearchResult = (props) => {

    const useStyles = makeStyles(theme => ({
        container: {
            display: 'flex',
            flexWrap: 'wrap',
            width: 500,
        },
        textField: {
            marginLeft: theme.spacing(1),
            marginRight: theme.spacing(1),
            width: '100%',
        },
        dense: {
            marginTop: theme.spacing(2),
        },
        menu: {
            width: 200,
        },
    }));

    const classes = useStyles();


    // console.log("SearchResult:init " + JSON.stringify(props.location.state))
    const [state, setState] = useState(props.location.state);

    let [query, setQuery] = useState(state.data.search.searchStr)
    // let inputRef = useRef();
    let [errorMessage, setErrorMessage] = useState()
    let [error, setError] = useState(false)

    // useEffect(() => {
    //     function initQuery() {
    //         inputRef.current.value = query
    //         inputRef.current.focus()
    //     }
    //     initQuery()
    // })

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            submitQuery();
        }
    }
    // const updateValue = (val) => {
    //     query = val
    // }

    let pageNumbers = []
    let currentPage = state.data.search.page
    let startPage = 1
    let endPage = state.data.search.pages <= 10 ? state.data.search.pages : 10
    if (currentPage > 10) {
        endPage = currentPage
        startPage = currentPage - 9
    }
    for (let i = startPage; i <= endPage; i++) {
        pageNumbers.push(i);
    }

    let renderPageNumbers = pageNumbers.map(number => {
        let classes = state.data.search.page === number ? 'active' : '';

        return (
            <span key={number} className={classes} onClick={() => makeHttpRequestWithPage(number)}>{number}</span>
        );
    });

    async function makeHttpRequestWithPage(pageNumber) {
        if (pageNumber !== 1) {

            if ((pageNumber > state.data.search.pages || !query) || (pageNumber < 1 || !query)) {
                return;
            }
        }

        state.data.search.searchStr = query;
        state.data.search.page = pageNumber;
        let resStatus = 0
        let queryJson = JSON.stringify(state.data.search)
        setError(false)
        setErrorMessage(null)
        fetch(`${BASE_URL}/search`,
            {
                method: "POST",
                headers: {
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },

                body: queryJson
            })
            .then(function (res) {
                resStatus = res.status
                return res.json();
            })
            .then(function (data) {

                switch (resStatus) {
                    case 200:
                        if (data.success) {
                            setState(state => ({ ...state, data: data.data }));
                            query = data.data.search.searchStr
                        } else {
                            setError(true)
                            setErrorMessage(data.data.searchStr)
                        }
                        break
                    case 400:
                        setError(true)
                        setErrorMessage(data.data.searchStr)
                        break
                    default:
                        console.log('unhandled')
                        break
                }

            })
    }

    function submitQuery() {
        // console.log("Query: " + query);
        makeHttpRequestWithPage(1);
    }

    return (
        <div>
            <Link to="/" >
                <Button variant="text" color="primary">
                    Back to Crawl
                </Button>
            </Link>

            <div className="container">
                <div className="seachResultsBox">

                    <div className="search-div">
                        <Typography variant="h4" color="primary">
                            <span >Search</span>
                        </Typography>
                        <TextField
                            label="Type query here to search in crawled results"
                            type="search"
                            className={classes.textField}
                            margin="normal"
                            variant="outlined"
                            onChange={(e) => setQuery(e.target.value)}
                            onKeyDown={(e) => handleKeyDown(e)}
                            width="400px"
                            disabled={false}
                            value={query}
                            autoFocus={true}
                            error={error}
                            helperText={errorMessage}
                        />
                        <Button variant="contained" color="primary" onClick={submitQuery} disabled={false}>
                            Go
            </Button>
                    </div>
                </div>


                {state.data.items && (<div className="searchResultItems">
                    {state.data.items.map(item => (
                        <div key={item.link} className="searhResultItemBox">
                            <UILink href={item.link} target="_blank" rel="noopener noreferrer">
                                <span dangerouslySetInnerHTML={{ __html: item.title }}></span>
                            </UILink>
                            <UILink href={item.link} target="_blank" rel="noopener noreferrer">
                                <Typography variant="caption" color="inherit">
                                    <span className="searchResultText"> {item.link.substring(0, 70) + '...'}</span>
                                </Typography>
                            </UILink>
                            <Typography variant="inherit" color="inherit">

                                <div className="searchResultText" dangerouslySetInnerHTML={{ __html: `<span style="font-size: small; color: gray">${item.date}&nbsp&nbsp;</span>` + item.context }}></div>
                            </Typography>
                        </div>
                    ))}
                </div>)
                }

                {state.data.search.total > 0 && (
                    < div className="pagination">
                        <Typography variant="inherit">
                            <span onClick={() => makeHttpRequestWithPage(state.data.search.page - 1)}>&laquo;</span>
                            {renderPageNumbers}
                            <span onClick={() => makeHttpRequestWithPage(state.data.search.page + 1)}>&raquo;</span>
                        </Typography>
                    </div>
                )
                }

            </div >

        </div>

    );
}

export default withRouter(SearchResult);








